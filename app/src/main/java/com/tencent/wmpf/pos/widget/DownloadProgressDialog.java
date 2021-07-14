package com.tencent.wmpf.pos.widget;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.wmpf.pos.R;
import com.tencent.wmpf.pos.base.BaseDialogFragment;

/**
 * Created by gaochujia on 2020-12-22.
 */

public class DownloadProgressDialog extends BaseDialogFragment {
    private AnimProgressBar progressView;
    private TextView tips;
    private LinearLayout rlFirst, rlSecond;
    private TextView cancel, download;
    private View.OnClickListener listener;

    @Override
    protected int getLayout() {
        return R.layout.dialog_download;
    }

    @Override
    protected void initView(View contentView) {
        super.initView(contentView);
        progressView = contentView.findViewById(R.id.download_progress);
        tips = contentView.findViewById(R.id.tips);
        cancel = contentView.findViewById(R.id.tv_cancel);
        download = contentView.findViewById(R.id.tv_download);
        rlFirst = contentView.findViewById(R.id.rl_first);
        rlSecond = contentView.findViewById(R.id.rl_second);

        getDialog().setCanceledOnTouchOutside(false);

        initListener();
    }
    
    private void initListener(){
        download.setOnClickListener(v -> {
            if (null != updateStartCallBack) {
                updateStartCallBack.start();
            }
        });
        cancel.setOnClickListener(v -> listener.onClick(v));
    }

    public void updateProgress(int progress) {
        if (progressView != null) {
            progressView.setProgress(progress);
        }
    }

    public void setTotal(int total) {
        if (progressView != null) {
            progressView.setMax(total);
        }
    }

    public void beginDownload(){
        rlSecond.setVisibility(View.VISIBLE);
        rlFirst.setVisibility(View.GONE);
    }

    public void reDownload() {
        rlSecond.setVisibility(View.GONE);
        rlFirst.setVisibility(View.VISIBLE);
    }

    public void downloadFinish() {
        rlSecond.setVisibility(View.GONE);
        rlFirst.setVisibility(View.VISIBLE);
        cancel.setVisibility(View.GONE);
        tips.setText("下载完成");
        download.setText("确定");
        download.setOnClickListener(v -> {
            dismiss();
        });
    }

    public void onCancel(View.OnClickListener listener) {
        this.listener = listener;
    }

    private UpdateStartCallBack updateStartCallBack;

    public void setUpdateStartCallBack(UpdateStartCallBack updateStartCallBack) {
        this.updateStartCallBack = updateStartCallBack;
    }

    public interface UpdateStartCallBack {
        void start();
    }

}