package com.rarnu.tools.root.fragment;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.rarnu.command.CommandCallback;
import com.rarnu.devlib.base.BaseFragment;
import com.rarnu.devlib.component.DataProgressBar;
import com.rarnu.tools.root.MainActivity;
import com.rarnu.tools.root.R;
import com.rarnu.tools.root.adapter.HardUpdateAdapter;
import com.rarnu.tools.root.common.DataappInfo;
import com.rarnu.tools.root.common.MenuItemIds;
import com.rarnu.tools.root.utils.ApkUtils;

public class HardUpdateFragment extends BaseFragment implements CommandCallback {

	ListView lvHardUpdate;
	DataProgressBar progressScanApk;
	HardUpdateAdapter adapter;
	List<DataappInfo> list;

	MenuItem itemRefresh;

	@Override
	public int getBarTitle() {
		return R.string.func_hardupdate;
	}

	@Override
	public int getBarTitleWithPath() {
		return R.string.func_hardupdate_with_path;
	}

	@Override
	public String getCustomTitle() {
		return null;
	}

	private Handler hButtonClick = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				DataappInfo info = (DataappInfo) msg.obj;
				switch (info.apkStatus) {
				case 0:
				case 3:
					doInstallAppT(info, msg.arg1, false);
					break;
				case 1:
					doInstallAppT(info, msg.arg1, true);
					break;
				}

			}
			super.handleMessage(msg);
		};
	};

	final Handler hInstalled = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				list.get(msg.arg2).installing = false;
				progressScanApk.setVisibility(View.GONE);
				if (msg.arg1 == 0) {
					list.get(msg.arg2).apkStatus = 2;
					Toast.makeText(getActivity(),
							R.string.install_update_apk_ok, Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(getActivity(),
							R.string.install_update_apk_fail, Toast.LENGTH_LONG)
							.show();
				}
				adapter.notifyDataSetChanged();
			}
			super.handleMessage(msg);
		}
	};

	private void doInstallAppT(final DataappInfo info, final int position,
			final boolean force) {
		list.get(position).installing = true;
		adapter.notifyDataSetChanged();
		progressScanApk.setAppName(getString(R.string.installing_updating_app));
		progressScanApk.setVisibility(View.VISIBLE);

		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean ret = force ? ApkUtils.forceInstallApp(getActivity(), info)
						: ApkUtils.installApp(info);
				Message msg = new Message();
				msg.what = 1;
				msg.arg1 = ret ? 0 : 1;
				msg.arg2 = position;
				hInstalled.sendMessage(msg);
			}
		}).start();
	}

	@Override
	protected void initComponents() {
		lvHardUpdate = (ListView) innerView.findViewById(R.id.lvHardUpdate);
		progressScanApk = (DataProgressBar) innerView
				.findViewById(R.id.progressScanApk);
		list = new ArrayList<DataappInfo>();
		adapter = new HardUpdateAdapter(getActivity(), list, hButtonClick);
		lvHardUpdate.setAdapter(adapter);
	}

	@Override
	protected void initEvents() {

	}

	@Override
	protected void initLogic() {

	}

	@Override
	protected int getFragmentLayoutResId() {
		return R.layout.layout_hardupdate;
	}

	@Override
	protected String getMainActivityName() {
		return MainActivity.class.getName();
	}

	@Override
	protected void initMenu(Menu menu) {
		itemRefresh = menu.add(0, MenuItemIds.MENU_REFRESH, 99,
				R.string.refresh);
		itemRefresh.setIcon(android.R.drawable.ic_menu_revert);
		itemRefresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MenuItemIds.MENU_REFRESH:
			progressScanApk.setAppName(getString(R.string.proc_scaning_apk));
			progressScanApk.setVisibility(View.VISIBLE);
			lvHardUpdate.setVisibility(View.VISIBLE);
			itemRefresh.setEnabled(false);
			list.clear();
			adapter.setEnableButtons(false);
			ApkUtils.scanApksInSdcard(this);
			break;
		}
		return true;
	}

	@Override
	protected void onGetNewArguments(Bundle bn) {

	}

	private Handler hReadLine = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				String line = (String) msg.obj;
				if (!line.contains("/.")) {

					DataappInfo newinfo = new DataappInfo();
					newinfo.localPath = line;
					newinfo.info = ApkUtils.getAppInfoFromPackage(line);
					newinfo.apkStatus = ApkUtils.getApkFileStatus(
							getActivity(), newinfo);
					newinfo.checked = false;
					list.add(newinfo);
					adapter.notifyDataSetChanged();
				}

			}
			super.handleMessage(msg);
		};
	};

	private Handler hFinish = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			progressScanApk.setVisibility(View.GONE);
			itemRefresh.setEnabled(true);
			adapter.setEnableButtons(true);
			super.handleMessage(msg);
		};
	};

	@Override
	public void onReadLine(String line) {
		Message msg = new Message();
		msg.what = 1;
		msg.obj = line;
		hReadLine.sendMessage(msg);
	}

	@Override
	public void onReadError(String line) {

	}

	@Override
	public void onCommandFinish() {
		hFinish.sendEmptyMessage(1);

	}

}
