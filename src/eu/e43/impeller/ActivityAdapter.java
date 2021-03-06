/* Copyright 2013 Owen Shepherd. A part of Impeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.e43.impeller;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivityAdapter extends BaseAdapter {
	static final String TAG = "ActivityAdapter";
	
	Feed 					m_feed;
	Feed.Listener       	m_listener;
	ActivityWithAccount		m_ctx;
	View					m_view;
	List<JSONObject>		m_items;
	
	public ActivityAdapter(ActivityWithAccount ctx, Feed feed) {
		m_feed = feed;
		m_ctx  = ctx;
		m_listener = new Feed.Listener() {
			
			@Override
			public void feedUpdated(Feed feed, int unread, List<JSONObject> items) {
				Log.i(TAG, "Feed updated! " + items.size());
				m_items = items;
				notifyDataSetChanged();
			}

			@Override
			public void updateStarted(Feed feed) {}			
		};
		
		m_feed.addListenerAndNotify(m_listener);
	}
	
	public void close() {
		m_feed.removeListener(m_listener);
	}

	@Override
	public int getCount() {
		return m_items.size();
	}

	@Override
	public Object getItem(int position) {
		return m_items.get(position);
	}

	private static String getImage(JSONObject obj) {
		JSONObject mediaLink = obj.optJSONObject("image");
		if(mediaLink == null) return null;
		
		return Utils.getImageUrl(mediaLink);
	}
	
	@Override
	public int getViewTypeCount()
	{
		return 3;
	}
	
	@Override
	public int getItemViewType(int pos)
	{
		JSONObject json = (JSONObject) getItem(pos);
		return getItemViewType(json);
	}
	
	public int getItemViewType(JSONObject act) {
		JSONObject obj = act.optJSONObject("object");
		if(obj == null) {
			return 0;
		} else if("image".equals(obj.optString("objectType"))) {
			return 2;
		} else if("note".equals(obj.optString("objectType"))) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@Override
	public View getView(int position, View v, ViewGroup parent) {
	    JSONObject json = (JSONObject) getItem(position);
	    int type = getItemViewType(json);
	    
	    switch(type) {
	    case 0:
	    	// Simple activity
	    	if(v == null) {
	    		LayoutInflater vi = LayoutInflater.from(m_ctx);
	    		v = vi.inflate(android.R.layout.simple_list_item_1, null);
	    	}
	    	
	    	TextView text = (TextView) v.findViewById(android.R.id.text1);
	    	text.setText(Html.fromHtml(json.optString("content", "(Missing")));
	    	break;
	    	
	    case 1:
	    	// Note
		    if (v == null) {
		        LayoutInflater vi = LayoutInflater.from(m_ctx);
		        v = vi.inflate(R.layout.post_view, null);
		    }

		    TextView   caption    = (TextView)   v.findViewById(R.id.caption);
		    TextView  description = (TextView)  v.findViewById(R.id.description);
		    ImageView image       = (ImageView) v.findViewById(R.id.image);
		    
			description.setText(Html.fromHtml(json.optString("content", "(Action string missing)")));
		    try {
		    	JSONObject obj = json.getJSONObject("object");
		    	String content = obj.getString("content");
		    	
		    	PumpHtml.setFromHtml(m_ctx, caption, content);
				
				m_ctx.getImageLoader().setImage(image, getImage(json.getJSONObject("actor")));
			} catch (JSONException e) {
				caption.setText(Html.fromHtml(e.getLocalizedMessage()));
				//caption.loadData(e.getLocalizedMessage(), "text/plain", "utf-8");
			}
		    break;
		    
	    case 2:
	    	// Image
	    	if(v == null) {
	    		LayoutInflater vi = LayoutInflater.from(m_ctx);
	    		v = vi.inflate(R.layout.image_view, null);
	    	}
	    	
	    	TextView imgDescription = (TextView)  v.findViewById(R.id.description);
	    	ImageView imgImg        = (ImageView) v.findViewById(R.id.imageImage);
	    	
	    	try {
	    		imgDescription.setText(Html.fromHtml(json.optString("content", "(Action string missing)")));
	    		m_ctx.getImageLoader().setImage(imgImg, getImage(json.getJSONObject("object")));
	    	} catch(JSONException e) {
	    		imgDescription.setText(e.getMessage());
	    	}
	    	break;
	    }
		
		return v;
	}

	@Override
	public long getItemId(int id) {
		return id;
	}
}
