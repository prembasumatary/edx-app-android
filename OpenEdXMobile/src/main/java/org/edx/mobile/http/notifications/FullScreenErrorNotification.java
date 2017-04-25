package org.edx.mobile.http.notifications;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.TextView;

import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.IconDrawable;

import org.edx.mobile.R;

/**
 * A persistent full screen notification error message.
 */
public class FullScreenErrorNotification extends ErrorNotification {
    /**
     * The view that we need to hide.
     */
    @NonNull
    private final View viewToHide;

    /**
     * The TextView that we need to update with the error message and show.
     */
    @NonNull
    private final TextView viewToShow;

    /**
     * Construct a new instance of the notification.
     *
     * @param viewToHide A view from the content layout, used to seek an appropriate anchor for the
     *             Snackbar.
     */
    public FullScreenErrorNotification(@NonNull final View viewToHide,
                                       @NonNull final TextView viewToShow) {
        this.viewToHide = viewToHide;
        this.viewToShow = viewToShow;
    }

    /**
     * Show the error notification as a persistent Snackbar, according to the provided details.
     *
     * @param errorResId The resource ID of the error message.
     * @param icon The error icon. This is ignored here, since Snackbar doesn't really support
     *             icons.
     * @param actionTextResId The resource ID of the action button text.
     * @param actionListener The callback to be invoked when the action button is clicked.
     */
    @Override
    public void showError(@StringRes final int errorResId,
                             @NonNull final Icon icon,
                             @StringRes final int actionTextResId,
                             @Nullable final View.OnClickListener actionListener) {
        viewToHide.setVisibility(View.GONE);
        Context context = viewToHide.getContext();
        viewToShow.setVisibility(View.VISIBLE);
        viewToShow.setText(context.getString(errorResId));
        viewToShow.setCompoundDrawablesWithIntrinsicBounds(null,
                new IconDrawable(context, icon)
                        .sizeRes(context, R.dimen.content_unavailable_error_icon_size)
                        .colorRes(context, R.color.edx_brand_gray_back),
                null, null
        );
    }
}
