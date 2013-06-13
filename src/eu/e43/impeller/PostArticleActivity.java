package eu.e43.impeller;

import android.accounts.Account;
import android.graphics.Typeface;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class PostArticleActivity extends ActivityWithAccount implements OnEditorActionListener, Callback {
	EditText   m_bodyEdit;
	ActionMode m_actionMode; 
	
	@Override
	protected void onCreateEx() {
		setContentView(R.layout.activity_post_article);
		setupActionBar();
		
		m_bodyEdit = (EditText) this.findViewById(R.id.body);
		m_bodyEdit.setOnEditorActionListener(this);
		m_bodyEdit.setCustomSelectionActionModeCallback(this);
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.post_article, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void gotAccount(Account a) {
		// TODO Auto-generated method stub
		
	}
	


	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		return false;
	}

	private boolean hasStyle(Editable ed, int bit, int start, int end) {
		boolean present = false;
		
		for(StyleSpan span : ed.getSpans(start, end, StyleSpan.class)) {
			present = present || (span.getStyle() & bit) != 0;
		}
		
		return present;
	}
	
	private void setStyleFlag(Editable ed, int bit, boolean state, int start, int end) {
		int mask = ~bit;
		int set  = state ? bit : 0; 
		
		StyleSpan[] spans = ed.getSpans(start, end, StyleSpan.class);
		
		// Deal with overhang: first and last
		if(spans.length > 0) {
			if(ed.getSpanStart(spans[0]) < start) {
				// Split
				ed.setSpan(new StyleSpan(spans[0].getStyle()),
						ed.getSpanStart(spans[0]), start, 
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				ed.setSpan(spans[0], 
						start, ed.getSpanEnd(spans[0]), 
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			
			if(ed.getSpanEnd(spans[spans.length - 1]) > end) {
				// Split
				StyleSpan span = spans[spans.length - 1];
				ed.setSpan(new StyleSpan(span.getStyle()), 
						end, ed.getSpanEnd(span), 
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				
				ed.setSpan(span, 
						ed.getSpanStart(span), end, 
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		
		if(spans.length == 1 && ed.getSpanStart(spans[0]) == start &&
				ed.getSpanEnd(spans[0]) == end) {
			// Only one span encompassing whole text - update
			
			int style = (spans[0].getStyle() & mask) | set;
			ed.removeSpan(spans[0]);
			if(style != 0) 
				ed.setSpan(new StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			
			return;
		}
		
		// Walk the spans and remove the property we are editing
		for(StyleSpan span : spans) {
			int style = span.getStyle() & mask;
			int spanStart = ed.getSpanStart(span);
			int spanEnd   = ed.getSpanEnd(span);
			ed.removeSpan(span);
			
			if(style != 0) {
				ed.setSpan(new StyleSpan(style), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		
		// Add a new span with our property
		if(set != 0)
			ed.setSpan(new StyleSpan(set), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	}
	
	private void toggleStyle(Editable ed, int bit, int start, int end) {
		boolean present = hasStyle(ed, bit, start, end);
		setStyleFlag(ed, bit, !present, start, end);
	}
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		CharacterStyle cs = null;
		int start   = m_bodyEdit.getSelectionStart();
		int end     = m_bodyEdit.getSelectionEnd();
		Editable ed = m_bodyEdit.getEditableText();
		
		switch(item.getItemId()) {
		case R.id.action_bold:
			toggleStyle(ed, Typeface.BOLD, start, end);
            return true;
            
		case R.id.action_italic:
			toggleStyle(ed, Typeface.ITALIC, start, end);
			return true;
			
		case R.id.action_underline:
			UnderlineSpan[] spans = ed.getSpans(start, end, UnderlineSpan.class);
			if(spans.length != 0) {
				for(UnderlineSpan span : spans)
					ed.removeSpan(span);
			} else {
				ed.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			return true;
            
        default:
        	return false;
		}
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		getMenuInflater().inflate(R.menu.editor_context, menu);
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return true;
	}

}
