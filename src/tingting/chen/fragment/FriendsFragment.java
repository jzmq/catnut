/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import tingting.chen.R;
import tingting.chen.adapter.UsersAdapter;
import tingting.chen.api.FriendshipsAPI;
import tingting.chen.metadata.User;
import tingting.chen.processor.UserProcessor;
import tingting.chen.tingting.TingtingProvider;
import tingting.chen.tingting.TingtingRequest;
import tingting.chen.util.Constants;
import tingting.chen.util.TingtingUtils;

/**
 * 好友界面
 *
 * @author longkai
 */
public class FriendsFragment extends UsersFragment {

	public static final String TAG = "FriendsFragment";

	private static final String[] COLUMNS = new String[]{
		BaseColumns._ID,
		User.screen_name,
		User.profile_image_url,
		User.verified,
		User.location,
		User.description,
		User.following,
		User.follow_me
	};

	private long mUid;
	/** 是加载关注好友呢，还是关注me的人咧 */
	private boolean mIsFollowing;

	public static FriendsFragment getInstance(long uid, boolean isFollowing) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, uid);
		args.putBoolean(TAG, isFollowing); // 这里，随便上一个string的key了，懒得定义Orz
		FriendsFragment fragment = new FriendsFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	protected void fetchFromCloud(int cursor) {
		int size = super.getDefaultFetchSize();
		mRequestQueue.add(new TingtingRequest(
			mActivity,
			mIsFollowing ? FriendshipsAPI.friends(mUid, size, cursor, 1)
				: FriendshipsAPI.followers(mUid, size, cursor, 1),
			new UserProcessor.UsersProcessor(),
			successListener,
			errorListener
		)).setTag(TAG);
	}

	@Override
	public void onStart() {
		super.onStart();
		mActivity.getActionBar().setTitle(
			getString(mIsFollowing ? R.string.my_followings_title : R.string.follow_me_title)
		);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new UsersAdapter(mActivity);
		Bundle args = getArguments();
		mUid = args.getLong(Constants.ID, 0L);
		mIsFollowing = args.getBoolean(TAG, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText("no data...");
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onStop() {
		mRequestQueue.cancelAll(TAG);
		super.onStop();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String limit = String.valueOf(getDefaultFetchSize() * (mCurPage + 1));
		StringBuilder where = new StringBuilder(
			mIsFollowing ? User.following : User.follow_me
		).append("=1");
		if (!TextUtils.isEmpty(mCurFilter) && mCurFilter.trim().length() != 0) {
			limit = null;
			where
				.append(" and (")
					.append(User.screen_name).append(" like ").append(TingtingUtils.like(mCurFilter))
					.append(" or ").append(User.description).append(" like ").append(TingtingUtils.like(mCurFilter))
				.append(")");
			if (!isSearching) {
				getListView().removeFooterView(mLoadMore);
			}
			isSearching = true;
		} else if (isSearching) {
			isSearching = false;
			getListView().addFooterView(mLoadMore);
		}

		CursorLoader cursorLoader = TingtingUtils.getCursorLoader(
			getActivity(),
			TingtingProvider.parse(User.MULTIPLE),
			COLUMNS,
			where.toString(),
			null,
			User.TABLE,
			null,
			BaseColumns._ID + " desc",
			limit
		);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}