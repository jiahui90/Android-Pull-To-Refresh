/**
 * Use this however you want, but it might not work how you'd like.
 * If you fix it or make it better, share the wealth.
 * 
 * GitHub timahoney
 */
package com.your.package;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A container for a list view and a view that you can pull to refresh.
 * This uses a default ListView and view for the header.
 * 
 */
public class PullRefreshContainerView extends ScrollView {	
	/**
	 * Interface for listening to when the refresh container changes state.
	 */
	public interface OnChangeStateListener {
		/**
		 * Notifies a listener when the refresh view's state changes.
		 * @param container The container that contains the header
		 * @param state The state of the header. May be STATE_IDLE, STATE_READY,
		 * 		or STATE_REFRESHING.
		 */
		public void onChangeState(PullRefreshContainerView container, int state);
	}

	/**
	 * State of the refresh header when it is doing nothing or being pulled down slightly.
	 */
	public static final int STATE_IDLE = 0;

	/**
	 * State of the refresh header when it has been pulled down enough to start refreshing, but
	 * has not yet been released.
	 */
	public static final int STATE_READY = 1;

	/**
	 * State of the refresh header when the list should be refreshing.
	 */
	public static final int STATE_LOADING = 2;

	private static final float DEFAULT_RESISTANCE = 1.5f;


	private View mHeaderContainer;
	private View mHeaderView;
	private ListView mList;
	private int mState;
	//private float mTouchY;
	private ViewGroup mContainer;
	private boolean mScrollingList;
	//private boolean mForceHeaderDown;
	private boolean mApplyResistance;
	private float mResistance;
	private OnChangeStateListener mOnChangeStateListener;
	//private boolean mWaitingForMove;
	private float mInterceptY;

	private boolean mNeedScroll;


	/**
	 * Creates a new pull to refresh container.
	 * 
	 * @param context the application context
	 */
	public PullRefreshContainerView(Context context) {
		super(context);
		init(context);
	}

	/**
	 * Creates a new pull to refresh container.
	 * 
	 * @param context the application context
	 * @param attrs the XML attribute set
	 */
	public PullRefreshContainerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	/**
	 * Creates a new pull to refresh container.
	 * 
	 * @param context the application context
	 * @param attrs the XML attribute set
	 * @param defStyle the style for this view
	 */
	public PullRefreshContainerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		mResistance = DEFAULT_RESISTANCE;

		// Add the list and the header to the container.
		mContainer = new LinearLayout(context);
		mContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		((LinearLayout) mContainer).setOrientation(LinearLayout.VERTICAL);
		addView(mContainer);

		mState = STATE_IDLE; // Start out as idle.

		// We don't want to see the fading edge on the container.
		setVerticalFadingEdgeEnabled(false);
		setVerticalScrollBarEnabled(false);

		// Set the default list and header.
		TextView headerView = new TextView(context);
		headerView.setText("Default refresh header.");
		int headerViewId = 15134;
		headerView.setId(headerViewId);
		LinearLayout headerContainer = new LinearLayout(context);
		headerContainer.setPadding(0, 100, 0, 0);
		headerContainer.addView(headerView);
		setRefreshHeader(headerContainer, headerViewId);
		ListView list = new ListView(context);
		setList(list);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (mList.getHeight() != getHeight()) {
			mList.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, getHeight()));
			mNeedScroll = true;
		}
		if (mContainer.getHeight() != (getHeight() + mHeaderContainer.getHeight())) {
			mContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, getHeight() + mHeaderContainer.getHeight()));
			mNeedScroll = true;
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (mNeedScroll) {
			if (getScrollY() == mHeaderContainer.getBottom()) {
				mNeedScroll = false;
			} else {
				hideRefreshView();
			}
		}
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		float oldLastY = mInterceptY;
		mInterceptY = ev.getY();
		
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (mList.getFirstVisiblePosition() == 0 && getScrollY() <= mHeaderContainer.getBottom()
					&& (mList.getChildCount() == 0 || mList.getChildAt(0).getTop() == 0)) {
				// We are already scrolled into the header.
				return super.onInterceptTouchEvent(ev);
			} else {
				// Check in MOVE for what we want to do.
				return false;
			}
			
		case MotionEvent.ACTION_MOVE:
			if (mList.getFirstVisiblePosition() == 0 && getScrollY() >= mHeaderContainer.getBottom()
					&& (mList.getChildCount() == 0 || mList.getChildAt(0).getTop() == 0)) {
				if (mInterceptY > oldLastY) {
					mScrollingList = false;
					return super.onInterceptTouchEvent(ev);
				} else {
					mScrollingList = true;
					return false;
				}
			} else if (mScrollingList) {
				return false;
			} else {
				return super.onInterceptTouchEvent(ev);
			}
		
		case MotionEvent.ACTION_UP:
			return !mScrollingList;
			
		default:
			return super.onInterceptTouchEvent(ev);
		}
	}

	// TODO Remove this. Previously, touch events were handled using dispatchTouchEvent.
	// This was a little more ambitious, but not as simple.
//	@Override
//	public boolean dispatchTouchEvent(MotionEvent ev) {
//		float oldLastY = mTouchY;
//		mTouchY = ev.getY();
//
//		switch (ev.getAction()) {
//		case MotionEvent.ACTION_DOWN:
//			// All down events go to the list.
//			// If the user wants to pull down, we will send a fake down event to our scroller.
//			if (mState == STATE_IDLE && 
//					getScrollY() > mHeaderContainer.getBottom() || mForceHeaderDown) {
//				//Log.d("TOUCH DOWN HEADER");
//				// If we are already viewing the header, we want touches to go to the container.
//				// Also, this event may have come from a move at the border.
//				// In this case, mForceHeaderDown would be true, and we want to start pulling down.
//				// We dispatch this event to the container directly through onTouchEvent, otherwise
//				// the event may go to the list, and we don't want that.
//				mScrollingList = false;
//				mForceHeaderDown = false;
//				return super.onTouchEvent(ev);
//			} else {
//				//Log.d("TOUCH DOWN LIST");
//				mScrollingList = true;
//				return mList.dispatchTouchEvent(ev);
//			}
//
//		case MotionEvent.ACTION_MOVE:
//			if (mScrollingList) {
//				// Currently scrolling the list.
//				// The most common situation is that we are not at the top.
//				if (mState != STATE_LOADING && mList.getFirstVisiblePosition() == 0 && 
//						(mList.getChildCount() == 0 || mList.getChildAt(0).getTop() == 0)
//						&& mTouchY > oldLastY) {
//					// We are at the top and pulling down. 
//					// Switch from moving the list to moving the container.
//					// We fire a cancel event to the list to stop anything there.
//					// If we were to fire an ACTION_UP instead, then it would act as a click.
//					MotionEvent listUp = MotionEvent.obtain(ev);
//					listUp.setAction(MotionEvent.ACTION_CANCEL);
//					mForceHeaderDown = true;
//					mScrollingList = false;
//					mList.dispatchTouchEvent(listUp);
//					MotionEvent scrollDown = MotionEvent.obtainNoHistory(ev);
//					scrollDown.setAction(MotionEvent.ACTION_DOWN);
//					dispatchTouchEvent(scrollDown);
//					MotionEvent newMove = MotionEvent.obtain(ev);
//					//Log.d("MOVE WAS LIST");
//					return super.dispatchTouchEvent(newMove);
//				} else {
//					// We are not showing the first child.
//					// Or we are showing the first child but the first child above the top.
//					// Or we are showing the first child at the top, but we are pushing up.
//					// We want to scroll the list.
//					//Log.d("MOVE LIST");
//					return mList.dispatchTouchEvent(ev);
//				}
//			} else {
//				// Currently not scrolling the list.
//				// If we are refreshing, or if we are at the border, just 
//				// If the header is showing, keep scrolling the container.
//				// Otherwise, determine whether we are trying to scroll the list again.
//				if (getScrollY() == mHeaderContainer.getBottom() && mTouchY < oldLastY) {
//					// The header is completely hidden and the user is pushing up.
//					// We want to start scrolling the list again.
//					MotionEvent scrollUp = MotionEvent.obtain(ev);
//					scrollUp.setAction(MotionEvent.ACTION_UP);
//					dispatchTouchEvent(scrollUp);
//					MotionEvent listDown = MotionEvent.obtainNoHistory(ev);
//					listDown.setAction(MotionEvent.ACTION_DOWN);
//					mScrollingList = true;
//					mList.dispatchTouchEvent(listDown);
//					MotionEvent newMove = MotionEvent.obtain(ev);
//					//Log.d("MOVE WAS HEADER");
//					return mList.dispatchTouchEvent(newMove);
//				} else {
//					// We are scrolling the header. Remember to apply some resistance.
//					// Also make sure we are currently in the correct state.
//					mApplyResistance = true;
//					if (mState == STATE_IDLE && getScrollY() <= mHeaderView.getTop()) {
//						mState = STATE_READY;
//						notifyStateChanged();
//					} else if (mState == STATE_READY && getScrollY() > mHeaderView.getTop()) {
//						mState = STATE_IDLE;
//						notifyStateChanged();
//					}
//					//Log.d("MOVE HEADER");
//					return super.dispatchTouchEvent(ev);
//				}
//			}
//
//		case MotionEvent.ACTION_UP:
//		case MotionEvent.ACTION_CANCEL:
//			mList.setVerticalScrollBarEnabled(true);
//			if (mScrollingList) {
//				//Log.d("UP LIST");
//				// We were scrolling the list, now we stopped.
//				mScrollingList = false;
//				hideRefreshView(); // Make sure the refresh view is hidden.
//				return mList.dispatchTouchEvent(ev);
//			} else {
//				// The user lifted their finger up while scrolling the header.
//				// If the user is not refreshing and they pulled past the header,
//				// then we want to refresh.
//				if (mState == STATE_READY) {
//					showHeader();
//					refresh();
//				} else {
//					hideRefreshView();
//				}
//
//				//Log.d("UP HEADER");
//				return super.dispatchTouchEvent(ev);
//			}
//
//		default:
//			//Log.d("DEFAULT");
//			return super.dispatchTouchEvent(ev);
//		}
//	}

//	@Override
//	public void fling(int velocityY) {
//		// Do nothing.
//	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_MOVE:
			mApplyResistance = true;
			if (mState == STATE_IDLE && getScrollY() <= mHeaderView.getTop()) {
				mState = STATE_READY;
				notifyStateChanged();
			} else if (mState == STATE_READY && getScrollY() > mHeaderView.getTop()) {
				mState = STATE_IDLE;
				notifyStateChanged();
			}
			return super.onTouchEvent(ev);
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			// We override this so we stop any flinging of the scroller.
			// Usually, an ACTION_UP event in a scroll view would result in some extra scrolling
			// because the user wants to fling through the scroller. However, in our case, we 
			// want to handle all touch up events.
			// We also want to hide the refresh view.
			if (mState == STATE_READY) {
				showHeader();
				refresh();
			} else {
				hideRefreshView();
			}
			return true;
		default:
			return super.onTouchEvent(ev);
		}
	}

	/**
	 * Sets the list to be used in this pull to refresh container.
	 * @param list the list to use
	 */
	public void setList(ListView list) {
		if (mList != null) {
			mContainer.removeView(mList);
		}
		mList = list;
		if (mList.getParent() != null) {
			ViewGroup parent = (ViewGroup) mList.getParent();
			parent.removeView(mList);
		}
		
		mList.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, getHeight()));
		mContainer.addView(mList, 1);
	}

	/**
	 * Sets a resistance for when the user is pulling down.
	 * 
	 * @param resistance The pulling resistance. The default is 1.5. 
	 * The higher this value, the more resistance the user will feel when pulling.
	 */
	public void setResistance(float resistance) {
		mResistance = resistance;
	}

	/**
	 * @return the list inside this pull to refresh container
	 */
	public ListView getList() {
		return mList;
	}

	/**
	 * Sets the view to use as the refresh header. 
	 * <p />
	 * Two views are involved, the header view and 
	 * the header container. The header view is the view at the top that will show while the list
	 * is refreshing. Usually, this will be a simple rectangle that says "refreshing" and the like.
	 * <p />
	 * The header container is the entire view that will be shown while the user is pulling down.
	 * Usually, the header container is a simple wrapper for the header view that has some padding 
	 * at the top. That way, the user can pull down a little further than the header view.
	 * <p />
	 * If the header view is the header container, then there will be no padding at the top, 
	 * and the user will need to scroll to the very top in order to refresh.
	 * 
	 * @param headerContainer the view to use as the whole header container.
	 * @param headerView the ID of the view inside this container that is the header that should should while loading
	 */
	public void setRefreshHeader(View headerContainer, int headerId) {
		if (mHeaderContainer != null) {
			mContainer.removeView(mHeaderContainer);
		}		

		if (headerContainer == null) {
			throw new RuntimeException("Please supply a non-null header container.");
		}
		else if (headerContainer.getId() != headerId) {
			// The header container is not the same as the header view.
			// Make sure the container contains the header view.
			if (!(headerContainer instanceof ViewGroup)) {
				throw new RuntimeException(
						"The header container view supplied is not a container. " +
						"Please supply a header container that is a subclass of ViewGroup. " + 
				"Or supply a header view ID that is the same as the container's.");			
			} else {
				mHeaderContainer = headerContainer;
				mHeaderView = ((ViewGroup) mHeaderContainer).findViewById(headerId);
				if (mHeaderView == null) {
					throw new RuntimeException("The header view ID supplied was not part of " +
					"the supplied header container.");
				}
			}
		} else {
			mHeaderView = mHeaderContainer = headerContainer;
		}

		mContainer.addView(mHeaderContainer, 0);
		requestLayout();
	}

	public void refresh() {	
		mState = STATE_LOADING;
		showHeader();	
		notifyStateChanged();		
	}

	/**
	 * Notifies the pull-to-refresh view that the refreshing is complete.
	 * This will hide the refreshing header.
	 */
	public void completeRefresh() {
		mState = STATE_IDLE;
		hideRefreshView();
		notifyStateChanged();
	}

	/**
	 * Notifies the listener that the state has changed.
	 */
	private void notifyStateChanged() {
		if (mOnChangeStateListener != null) {
			mOnChangeStateListener.onChangeState(this, mState);
		}
	}

	/**
	 * Hides the header at the top if it is visible.
	 */
	protected void hideRefreshView() {
		smoothScrollTo(0, mHeaderContainer.getBottom());
	}

	/**
	 * Scrolls to the top of the header.
	 */
	protected void showHeader() {
		smoothScrollTo(0, mHeaderView.getTop());
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);

		if (mApplyResistance) {
			// Simulate resistance on the pull down.
			// We have to round the result because otherwise we would never reach the end.
			// Vary the resistance depending on how close we are to the top.
			// We need to set the mApplyResistance to false before scrolling again, otherwise
			// we would get an overflow of recursive calls.
			mApplyResistance = false;
			int changeY = t - oldt;
			int headerBottom = mHeaderContainer.getBottom();
			float resistance = (mResistance * (headerBottom - t) / headerBottom) + 1;
			int newChangeY = (int) (changeY / (resistance * resistance));
			int newt = oldt + newChangeY;
			scrollTo(0, newt);
		}
	}

	/**
	 * @param listener the listener to be notified when the header state should change
	 */
	public void setOnChangeStateListener(OnChangeStateListener listener) {
		mOnChangeStateListener = listener;
	}
}
