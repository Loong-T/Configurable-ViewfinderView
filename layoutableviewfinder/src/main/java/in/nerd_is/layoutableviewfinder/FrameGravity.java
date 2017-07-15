package in.nerd_is.layoutableviewfinder;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static in.nerd_is.layoutableviewfinder.ViewfinderView.GRAVITY_CENTER;
import static in.nerd_is.layoutableviewfinder.ViewfinderView.GRAVITY_CENTER_HORIZONTAL;
import static in.nerd_is.layoutableviewfinder.ViewfinderView.GRAVITY_CENTER_VERTICAL;
import static in.nerd_is.layoutableviewfinder.ViewfinderView.GRAVITY_NONE;

/**
 * @author Xuqiang ZHENG on 17/7/14.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef(value = {
        GRAVITY_NONE,
        GRAVITY_CENTER,
        GRAVITY_CENTER_HORIZONTAL,
        GRAVITY_CENTER_VERTICAL
})
public @interface FrameGravity {
}
