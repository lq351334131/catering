package android.support.v4.app;

/**
 * Created by gaochujia on 2020-09-15.
 */

public class MyDialogFragment extends DialogFragment {

    /**
     * Display the dialog, adding the fragment to the given FragmentManager.
     *
     * @param manager        The FragmentManager this fragment will be added to.
     * @param tag            The tag for this fragment, as per
     *                       {@link FragmentTransaction#add(Fragment, String) FragmentTransaction.add}.
     * @param allowStateLoss allow state loss {@link FragmentTransaction#commitAllowingStateLoss()
     *                       FragmentTransaction.commitAllowingStateLoss()}.
     */
    @SuppressWarnings("unused")
    public void show(FragmentManager manager, String tag, boolean allowStateLoss) {
        mDismissed = false;
        mShownByMe = true;
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        if (allowStateLoss) {
            ft.commitAllowingStateLoss();
        } else {
            ft.commit();
        }
    }
}
