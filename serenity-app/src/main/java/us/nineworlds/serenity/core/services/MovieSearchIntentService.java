/**
 * The MIT License (MIT)
 * Copyright (c) 2012 David Carver
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package us.nineworlds.serenity.core.services;

import us.nineworlds.plex.rest.model.impl.MediaContainer;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * @author dcarver
 *
 * @deprecated Need to migrate this to Volley for searching for movies.
 */
@Deprecated
public class MovieSearchIntentService extends MoviesRetrievalIntentService {

	protected String query;

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle bundle = intent.getExtras();
		if (bundle == null) {
			Log.e(getClass().getName(), "Missing intent extras");
			return;
		}
		key = intent.getExtras().getString("key", "");
		query = intent.getExtras().getString("query");
		createPosters();
		sendMessageResults(intent);
	}

	@Override
	protected MediaContainer retrieveVideos() throws Exception {
		return factory.searchMovies(key, query);
	}

}
