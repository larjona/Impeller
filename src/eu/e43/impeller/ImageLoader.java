package eu.e43.impeller;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import android.accounts.Account;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;
import eu.e43.impeller.account.OAuth;

public class ImageLoader {
	static final String TAG = "ImageLoader";
	Context       m_ctx;
	Account		  m_account;
	
	public interface Listener {
		public void loaded(Drawable dr, URI uri);
		public void error(URI uri);
	}
	
	public ImageLoader(Context ctx, Account acct) {
		m_ctx       = ctx;
		m_account	= acct;
	}
	
	public void load(Listener l, URI uri) {
		Drawable dw = ms_images.get(uri);
		if(dw != null) 
			l.loaded(dw,  uri);
		
		FetchTask task = ms_tasks.get(uri);
		if(task == null) {
			task = new FetchTask();
			ms_tasks.put(uri,  task);
			task.m_listeners.add(l);
			task.execute(uri);
		} else {
			task.m_listeners.add(l);
		}
	}
	
	public void load(Listener l, String uri) {
		try {
			load(l, new URI(uri));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void setImage(final ImageView view, URI uri) {
		view.setImageDrawable(m_ctx.getResources().getDrawable(R.drawable.ic_image_loading));
		ms_viewUris.put(view,  uri);
		load(new Listener() {
			@Override
			public void loaded(Drawable dr, URI uri) {
				if(uri.equals(ms_viewUris.get(view))) {
					view.setImageDrawable(dr);
					ms_viewUris.remove(uri);
				}
			}

			@Override
			public void error(URI uri) {
				if(uri.equals(ms_viewUris.get(view))) {
					view.setImageDrawable(m_ctx.getResources().getDrawable(R.drawable.ic_image_broken));
					ms_viewUris.remove(uri);
				}
			}
		}, uri);
	}
	
	public void setImage(ImageView view, String imageUrl) {
		try {
			setImage(view, new URI(imageUrl));
		} catch(URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Drawable getCachedImage(URI uri) {
		return ms_images.get(uri);
	}
	
	public Drawable getCachedImage(String url) {
		try {
			return getCachedImage(new URI(url));
		} catch(URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
	static LruCache<URI, BitmapDrawable> ms_images = new LruCache<URI, BitmapDrawable>(10 * 1024 * 1024) {
		@Override
		protected int sizeOf(URI key, BitmapDrawable value) { 
			Bitmap bmp = value.getBitmap();
			if(bmp != null) return bmp.getByteCount();
			return 0;
		}
	};
	private static HashMap<URI, FetchTask> ms_tasks = new HashMap<URI, FetchTask>();
	
	// Needed to prevent issues when a ListView/etc recycles an ImageView
	private static HashMap<ImageView, URI> ms_viewUris = new HashMap<ImageView, URI>();
	
	class FetchTask extends AsyncTask<Object, Void, BitmapDrawable> {
		private URI 			   m_uri;
		public ArrayList<Listener> m_listeners = new ArrayList<Listener>();
		
		@Override
		protected BitmapDrawable doInBackground(Object... params) {
			m_uri  = (URI) params[0];
			try {
				URL url = m_uri.toURL();
			
				HttpURLConnection conn = OAuth.fetchAuthenticated(m_ctx, m_account, url);
				BitmapDrawable dw = new BitmapDrawable(m_ctx.getResources(), conn.getInputStream());
				dw.setBounds(0, 0, dw.getIntrinsicWidth(), dw.getIntrinsicHeight());
				return dw;
			} catch(Exception ex) {
				Log.e(TAG, "Error getting " + m_uri, ex);
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(BitmapDrawable dw) {
			ms_tasks.remove(m_uri);
			if(dw != null) {				
				ms_images.put(m_uri, dw);
				for(Listener l : m_listeners) {
					l.loaded(dw,  m_uri);
				}	
			} else {
				for(Listener l : m_listeners) {
					l.error(m_uri);
				}
			}
		}
		
	};
}
