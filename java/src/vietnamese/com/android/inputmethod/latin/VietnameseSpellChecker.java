package vietnamese.com.android.inputmethod.latin;

public class VietnameseSpellChecker {
	
	public static boolean isVietnameseWord(StringBuilder sb) {
    	for (int i = sb.length() - 1; i >= 0; i--) {
    		int currentChar = sb.charAt(i);
    		for (int j = 0; j < NON_VIETNAMESE_CHARACTERS.length; j++) {
    			if (currentChar == NON_VIETNAMESE_CHARACTERS[j][0] || 
    				currentChar == NON_VIETNAMESE_CHARACTERS[j][0] + ('a' - 'A')) {
    				if (i >= NON_VIETNAMESE_CHARACTERS[j][1]) {
    					return false;
    				}
    			}    			
    		}
    	}
    	return true;
    }
	
	/**
	 * @param word từ cần điều chỉnh dấu
	 * @param accent 0 = tự động tìm, 1 = sắc, 2 = huyền, 3 = hỏi, 4 = ngã, 5 = nặng
	 */
	public static boolean adjustAccent(StringBuilder word, int accent, boolean isComposingWord) {
		// find the first character has an accent
		int wordLength = word.length();
		int firstVowelIndex = -1;
		int vowelSequenceLength = 0;
		int accentPosition = -1;
		int currentAccentPosition = -1;
		byte numberOfAccents = 0;
		int[] vowelIndexes = new int[3];
		boolean needToFix = false;
		
		for (int pos = 0; pos < wordLength; pos++) {
			int ch = (int)word.charAt(pos);
			// check if this character is a vowel
			int vowelPosition = -1;
			for (int i = 0; i < VN_VOWELS.length; i++) {
				if (ch == VN_VOWELS[i]) {
					vowelIndexes[vowelSequenceLength] = i;
					vowelSequenceLength++;
					vowelPosition = i;
					break;
				}
			}
			
			// yes, it's a vowel
			if (vowelPosition != -1) {
				if (firstVowelIndex == -1) firstVowelIndex = pos;
				// has it have an accent?
				if (accent == 0) {
					currentAccentPosition = vowelSequenceLength - 1;
					accent = vowelPosition % 6;
				}
				if (vowelPosition % 6 > 0) {
					numberOfAccents++;
				}
			} else if (firstVowelIndex != -1) {
				// stop finding vowels
				break;
			}
			
			if (vowelSequenceLength == 3) break;
		}
		needToFix = numberOfAccents > 1;
				
//		System.out.println("firstVowelPositionInWord = " + firstVowelPositionInWord);
//		System.out.println("Sequence length = " + vowelSequenceLength);
//		System.out.println("Accent = " + accent);
				
		// fix ươ case		
		if (vowelSequenceLength >= 2) {
			int firstVowel = word.charAt(firstVowelIndex);
			firstVowel = VN_VOWELS[(vowelIndexes[0] / 6) * 6];
			int secondVowel = word.charAt(firstVowelIndex + 1);
			secondVowel = VN_VOWELS[(vowelIndexes[1] / 6) * 6];
			boolean endWithConsonant = (firstVowelIndex + 2 < wordLength);
			
			if (firstVowel == 'u' || firstVowel == 'U') {
				if ((secondVowel == 'ơ' || secondVowel == 'Ơ') && endWithConsonant) {
					// uơ[...] -> ươ[...]
					vowelIndexes[0] = UW_INDEX + (firstVowel == 'u' ? 0 : 6);
					needToFix = true;
				} else if (secondVowel == 'ă' || secondVowel == 'Ă') {
					// uă -> ưa
					vowelIndexes[0] = UW_INDEX + (firstVowel == 'u' ? 0 : 6);
					vowelIndexes[1] = A_INDEX + (secondVowel == 'ă' ? 0 : 6);
					needToFix = true;
				}
			} else if (firstVowel == 'ư' || firstVowel == 'Ư') {
				if (secondVowel == 'ơ' || secondVowel == 'Ơ') {
					if (!endWithConsonant && (currentAccentPosition == 0 || isComposingWord)) {
						// ươ -> uơ
						vowelIndexes[0] = U_INDEX + (firstVowel == 'ư' ? 0 : 6);
						needToFix = true;
					}
				} else if (secondVowel == 'o' || secondVowel == 'O') {
					if (endWithConsonant) {
						// ưo[...] -> ươ[...]
						vowelIndexes[1] = OW_INDEX + (secondVowel == 'o' ? 0 : 6);
						needToFix = true;
					} else if (currentAccentPosition == 0 || isComposingWord) {
						// ưo -> uơ
						vowelIndexes[0] = U_INDEX + (firstVowel == 'ư' ? 0 : 6);
						vowelIndexes[1] = OW_INDEX + (secondVowel == 'o' ? 0 : 6);
						needToFix = true;
					}
				}
			}
		}
		
		// put it to correct position
		if ((accent > 0 && vowelSequenceLength > 1) || needToFix) {
			if (vowelSequenceLength == 3) {
    			int middleVowel = word.charAt(firstVowelIndex + 1);    			
	    		middleVowel = VN_VOWELS[(vowelIndexes[1] / 6) * 6];
    			if (middleVowel != 'y' && middleVowel != 'Y') {
    				accentPosition = 1;
    			} else {
    				accentPosition = 2;
    			}
    		} else if (vowelSequenceLength == 2) {
    			accentPosition = 0;
    			
    			if (firstVowelIndex > 0) {
    				int consonant = word.charAt(firstVowelIndex - 1);
    				int firstVowel = word.charAt(firstVowelIndex);
    				firstVowel = VN_VOWELS[(vowelIndexes[0] / 6) * 6];
    				
    				if ((consonant == 'Q' || consonant == 'q') ||
    					((consonant == 'G' || consonant == 'g') && (firstVowel == 'i' || firstVowel == 'I'))) {
    					accentPosition = 1;
    				} else if (firstVowelIndex + vowelSequenceLength < wordLength) { // co phu am phia sau
        				accentPosition = 1;
    				} else if (vowelIndexes[0] > VOWEL_WITH_BREVE && vowelIndexes[1] > VOWEL_WITH_BREVE) { // 2 nguyên âm có dấu, bỏ dấu nguyên âm thứ 2
    					accentPosition = 1;
    				} else if (vowelIndexes[1] > VOWEL_WITH_BREVE) {
    					accentPosition = 1;
    				}
    			} else if (firstVowelIndex + vowelSequenceLength < wordLength - 1) { // co phu am phia sau
    				accentPosition = 1;
    			}
    		} else {
    			accentPosition = firstVowelIndex + vowelSequenceLength - 1;
    		}

//			System.out.println("Accent position = " + accentPosition);
			
			if (currentAccentPosition != accentPosition || needToFix) {
				for (int i = 0; i < vowelSequenceLength; i++) {
					int row = vowelIndexes[i] / 6;
		    		int replacedChar = VN_VOWELS[row * 6 + (i == accentPosition ? accent : 0)];
		    		word.setCharAt(firstVowelIndex + i, (char)replacedChar);
				}
				
				return true;
			}						
//		} else {
//			System.out.println("Accent not found!");
		}				
		
		return false;
	}		
	
	public static final int[] VN_VOWELS = {
    	'a', 'á', 'à', 'ả', 'ã', 'ạ',
    	'A', 'Á', 'À', 'Ả', 'Ã', 'Ạ',
    	
    	'e', 'é', 'è', 'ẻ', 'ẽ', 'ẹ',
    	'E', 'É', 'È', 'Ẻ', 'Ẽ', 'Ẹ',
    	
    	'o', 'ó', 'ò', 'ỏ', 'õ', 'ọ',
    	'O', 'Ó', 'Ò', 'Ỏ', 'Õ', 'Ọ',
    	
    	'u', 'ú', 'ù', 'ủ', 'ũ', 'ụ',
    	'U', 'Ú', 'Ù', 'Ủ', 'Ũ', 'Ụ',
    	
    	'i', 'í', 'ì', 'ỉ', 'ĩ', 'ị',
    	'I', 'Í', 'Ì', 'Ỉ', 'Ĩ', 'Ị',
    	
    	'y', 'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ',    	
    	'Y', 'Ý', 'Ỳ', 'Ỷ', 'Ỹ', 'Ỵ',
    	
    	'â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ',
    	'Â', 'Ấ', 'Ầ', 'Ẩ', 'Ẫ', 'Ậ',
    	
    	'ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ',
    	'Ă', 'Ắ', 'Ằ', 'Ẳ', 'Ẵ', 'Ặ',
    	
    	'ê', 'ế', 'ề', 'ể', 'ễ', 'ệ',
    	'Ê', 'Ế', 'Ề', 'Ể', 'Ễ', 'Ệ',
    	
    	'ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ',
    	'Ô', 'Ố', 'Ồ', 'Ổ', 'Ỗ', 'Ộ',
    	
    	'ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ',
    	'Ơ', 'Ớ', 'Ờ', 'Ở', 'Ỡ', 'Ợ',
    	
    	'ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự',    	
    	'Ư', 'Ứ', 'Ừ', 'Ử', 'Ữ', 'Ự'    	    	   	    	    	
    };
	
	private static final int[][] NON_VIETNAMESE_CHARACTERS = {
    	{'B', 1}, {'D', 1}, {'F', 0}, {'J', 0}, {'K', 1}, {'L', 1}, {'Q', 1}, 
    	{'R', 2}, {'S', 1}, {'V', 1}, {'W', 0}, {'X', 1}, {'Z', 0}
    };
		
	private static final int A_INDEX = 0;
	private static final int U_INDEX = 6 * 6;
	private static final int OW_INDEX = 20 * 6;		
	private static final int UW_INDEX = 22 * 6;
	
	private static final int VOWEL_WITH_BREVE = 12 * 6;
	
	public static final int ACCENT_AUTO = 0;
}
