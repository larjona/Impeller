package eu.e43.impeller;

import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import eu.e43.impeller.account.OAuth;

public class CommentAdapter extends BaseAdapter {
	private static final String TAG = "CommentAdapter";
	private JSONObject m_collection;
	private JSONArray m_comments;
	private ActivityWithAccount  m_ctx;
	
	public CommentAdapter(ActivityWithAccount act, JSONObject collection, boolean forceUpdate) {
		m_ctx = act;
		m_collection = collection;
		m_comments = collection.optJSONArray("items");
		if(m_comments == null)
			m_comments = new JSONArray();
		
		updateComments();
	}
	
	public void updateComments() {
		// Try for proxy_url
		JSONObject pumpIo = m_collection.optJSONObject("pump_io");
		if(pumpIo != null && pumpIo.has("proxyURL")) {
			CommentFetchTask t = new CommentFetchTask();
			t.execute(pumpIo.optString("proxyURL"));
		} else if(m_collection.has("url")) {
			CommentFetchTask t = new CommentFetchTask();
			t.execute(m_collection.optString("url"));
		}
	}
	
	private class CommentFetchTask extends AsyncTask<String, Void, JSONArray> {
		@Override
		protected JSONArray doInBackground(String... url_) {
			String urlString = url_[0];
			try {
				URL url = new URL(urlString);
				HttpURLConnection conn = OAuth.fetchAuthenticated(m_ctx, m_ctx.m_account, url);
				
				if(conn.getResponseCode() != 200) {
					Log.e(TAG, "Error getting comments" + Utils.readAll(conn.getErrorStream()));
					return null;
				}
				
				JSONObject collection = new JSONObject(Utils.readAll(conn.getInputStream()));
				return collection.optJSONArray("items");				
			} catch (Exception e) {
				Log.e(TAG, "Error fetching complete comments", e);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(JSONArray items) {
			if(items != null) {
				m_comments = items;
				notifyDataSetChanged();
			}
		}
		
	}
	
	@Override
	public int getCount() {
		return m_comments.length();
	}

	@Override
	public JSONObject getItem(int idx) {
		Log.v(TAG, "getItem(" + idx + ")");
		try {
			return m_comments.getJSONObject(m_comments.length() - idx - 1);
		} catch(JSONException ex) {
			Log.e(TAG, "Error getting comment", ex);
			return new JSONObject();
		}
	}

	@Override
	public long getItemId(int idx) {
		return idx;
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		Log.v(TAG, "getView(" + position + ")");
		if(v == null) {
			LayoutInflater vi = LayoutInflater.from(m_ctx);
			v = vi.inflate(R.layout.comment_view, null);
		}
		
		JSONObject comment = getItem(position);
		
		ImageView authorImage  = (ImageView) v.findViewById(R.id.commentAuthorImage);
		TextView  commentBody  = (TextView)  v.findViewById(R.id.commentBody);
		TextView  commentMeta  = (TextView)  v.findViewById(R.id.commentMeta);
		TextView  commentState = (TextView)  v.findViewById(R.id.commentState);
		
		JSONObject author = comment.optJSONObject("author");
		if(author != null) {
			JSONObject image = author.optJSONObject("image");
			if(image != null)
				m_ctx.getImageLoader().setImage(authorImage, Utils.getImageUrl(image));
			commentMeta.setText("By " + author.optString("displayName") + " at " + comment.optString("published"));
		}
		
		JSONObject likes = comment.optJSONObject("likes");
		if(likes != null) {
			int items = likes.optInt("totalItems", 0);
			//commentState.setVisibility(View.VISIBLE);
			if(comment.optBoolean("liked", false)) {
				// You and N other people like this
				if(items > 0) {
					commentState.setText("You and " + (items - 1) + " other people like this");
				} else {
					commentState.setText("You like this");
				}
			} else {
				// N people like this
				if(items > 0) {
					commentState.setText(items + " people like this");
				} else {
					commentState.setText("Nobody likes this =(");
					//commentState.setVisibility(View.INVISIBLE);
				}
			}
		}
		
		PumpHtml.setFromHtml(m_ctx, commentBody, comment.optString("content"));
		
		return v;
	}

}
