package com.davidbuzatu.schedly.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.activity.SettingsActivity;

public class ViewPolicyFragment extends Fragment {
    private FragmentActivity mActivity;
    private View mInflater;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater.inflate(R.layout.fragment_view_policy, container, false);

        WebView _webView = mInflater.findViewById(R.id.frag_VPolicy_WV);
        _webView.loadUrl("https://davidbuzatu-marian.github.io");
        return mInflater;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        ((SettingsActivity) mActivity).setActionBarTitle(mActivity.getString(R.string.settings_policy_bar_title));

    }


}
