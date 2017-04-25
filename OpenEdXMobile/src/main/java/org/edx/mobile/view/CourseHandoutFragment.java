package org.edx.mobile.view;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Xml.Encoding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.google.inject.Inject;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import org.edx.mobile.R;
import org.edx.mobile.base.BaseFragment;
import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.event.NetworkConnectivityChangeEvent;
import org.edx.mobile.http.callback.ErrorHandlingOkCallback;
import org.edx.mobile.http.notifications.FullScreenErrorNotification;
import org.edx.mobile.http.provider.OkHttpClientProvider;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.edx.mobile.model.api.HandoutModel;
import org.edx.mobile.module.analytics.Analytics;
import org.edx.mobile.module.analytics.AnalyticsRegistry;
import org.edx.mobile.util.NetworkUtil;
import org.edx.mobile.util.WebViewUtil;
import org.edx.mobile.view.custom.URLInterceptorWebViewClient;

import de.greenrobot.event.EventBus;
import okhttp3.Request;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;

public class CourseHandoutFragment extends BaseFragment {
    protected final Logger logger = new Logger(getClass().getName());

    @InjectExtra(Router.EXTRA_COURSE_DATA)
    private EnrolledCoursesResponse courseData;

    @Inject
    private AnalyticsRegistry analyticsRegistry;

    @Inject
    private IEdxEnvironment environment;

    @Inject
    private OkHttpClientProvider okHttpClientProvider;

    @InjectView(R.id.webview)
    private WebView webview;

    @InjectView(R.id.no_coursehandout_tv)
    private TextView errorTextView;

    private FullScreenErrorNotification errorNotification;

    private boolean isHandoutFetched;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        analyticsRegistry.trackScreenView(Analytics.Screens.COURSE_HANDOUTS, courseData.getCourse().getId(), null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        return inflater.inflate(R.layout.fragment_handout, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        errorNotification = new FullScreenErrorNotification(webview, errorTextView);
        new URLInterceptorWebViewClient(getActivity(), webview).setAllLinksAsExternal(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadData();
    }

    private void loadData() {
        okHttpClientProvider.getWithOfflineCache().newCall(new Request.Builder()
                .url(courseData.getCourse().getCourse_handouts())
                .get()
                .build())
                .enqueue(new ErrorHandlingOkCallback<HandoutModel>(getActivity(),
                        HandoutModel.class, errorNotification) {
                    @Override
                    protected void onResponse(@NonNull final HandoutModel result) {
                        if (getActivity() == null) {
                            return;
                        }

                        if (!TextUtils.isEmpty(result.handouts_html)) {
                            populateHandouts(result);
                        } else {
                            errorNotification.showError(R.string.no_handouts_to_display,
                                    FontAwesomeIcons.fa_exclamation_circle, 0, null);
                        }
                        isHandoutFetched = true;
                    }

                    @Override
                    protected void onFailure(@NonNull final Throwable error) {
                        super.onFailure(error);

                        if (getActivity() == null) {
                            return;
                        }

                        isHandoutFetched = false;
                    }
                });
    }

    private void populateHandouts(HandoutModel handout) {
        hideErrorMessage();

        StringBuilder buff = WebViewUtil.getIntialWebviewBuffer(getActivity(), logger);

        buff.append("<body>");
        buff.append("<div class=\"header\">");
        buff.append(handout.handouts_html);
        buff.append("</div>");
        buff.append("</body>");

        webview.clearCache(true);
        webview.loadDataWithBaseURL(environment.getConfig().getApiHostURL(), buff.toString(),
                "text/html", Encoding.UTF_8.toString(), null);

    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NetworkConnectivityChangeEvent event) {
        if (!isHandoutFetched) {
            if (NetworkUtil.isConnected(getContext())) {
                hideErrorMessage();
                loadData();
            } else {
                errorNotification.showError(R.string.reset_no_network_message,
                        FontAwesomeIcons.fa_wifi, 0, null);
            }
        }
    }

    private void hideErrorMessage() {
        webview.setVisibility(View.VISIBLE);
        errorTextView.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }
}
