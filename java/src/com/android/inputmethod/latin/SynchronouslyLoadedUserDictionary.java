/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.latin;

import android.content.Context;

import com.android.inputmethod.keyboard.ProximityInfo;
import vietnamese.com.android.inputmethod.latin.R;

public class SynchronouslyLoadedUserDictionary extends UserDictionary {
    private boolean mClosed;

    public SynchronouslyLoadedUserDictionary(final Context context, final String locale) {
        this(context, locale, false);
    }

    public SynchronouslyLoadedUserDictionary(final Context context, final String locale,
            final boolean alsoUseMoreRestrictiveLocales) {
        super(context, locale, alsoUseMoreRestrictiveLocales);
    }

    @Override
    public synchronized void getWords(final WordComposer codes,
            final CharSequence prevWordForBigrams, final WordCallback callback,
            final ProximityInfo proximityInfo) {
        blockingReloadDictionaryIfRequired();
        getWordsInner(codes, prevWordForBigrams, callback, proximityInfo);
    }

    @Override
    public synchronized boolean isValidWord(CharSequence word) {
        blockingReloadDictionaryIfRequired();
        return super.isValidWord(word);
    }

    // Protect against multiple closing
    @Override
    public synchronized void close() {
        if (mClosed) return;
        mClosed = true;
        super.close();
    }
}
