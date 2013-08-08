/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.incallui;

import com.google.common.base.Preconditions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 *
 */
public class AnswerFragment extends BaseFragment<AnswerPresenter> implements
        GlowPadWrapper.AnswerListener, AnswerPresenter.AnswerUi {

    /**
     * The popup showing the list of canned responses.
     *
     * This is an AlertDialog containing a ListView showing the possible
     * choices.  This may be null if the InCallScreen hasn't ever called
     * showRespondViaSmsPopup() yet, or if the popup was visible once but
     * then got dismissed.
     */
    private Dialog mCannedResponsePopup = null;

    private ArrayAdapter<String> mTextResponsesAdapter = null;

    public AnswerFragment() {
    }

    @Override
    public AnswerPresenter createPresenter() {
        return new AnswerPresenter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final GlowPadWrapper glowPad = (GlowPadWrapper) inflater.inflate(R.layout.answer_fragment,
                container, false);

        glowPad.setAnswerListener(this);
        getPresenter().onUiReady(this);

        return glowPad;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getPresenter().onUiUnready(this);
    }

    @Override
    public void showAnswerUi(boolean show) {
        getView().setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showTextButton(boolean show) {
        // TODO(klp) Hide the text button when the call does not support reject by text.
    }

    @Override
    public boolean isMessageDialogueShowing() {
        return mCannedResponsePopup != null && mCannedResponsePopup.isShowing();
    }

    @Override
    public void showMessageDialogue() {
        final ListView lv = new ListView(getActivity());

        Preconditions.checkNotNull(mTextResponsesAdapter);
        lv.setAdapter(mTextResponsesAdapter);
        lv.setOnItemClickListener(new RespondViaSmsItemClickListener());

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setView(lv);
        mCannedResponsePopup = builder.create();
        mCannedResponsePopup.show();
    }

    /**
     * Dismiss currently visible popups.
     *
     * This is safe to call even if the popup is already dismissed, and
     * even if you never called showRespondViaSmsPopup() in the first
     * place.
     */
    @Override
    public void dismissPopup() {
        if (mCannedResponsePopup != null) {
            mCannedResponsePopup.dismiss();  // safe even if already dismissed
            mCannedResponsePopup = null;
        }
    }

    @Override
    public void configureMessageDialogue(ArrayList<String> textResponses) {
        textResponses.add(getResources().getString(R.string.respond_via_sms_custom_message));
        mTextResponsesAdapter = new ArrayAdapter<String>(getActivity(),
                                         android.R.layout.simple_list_item_1,
                                         android.R.id.text1,
                                         textResponses);
    }

    @Override
    public void onAnswer() {
        getPresenter().onAnswer();
    }

    @Override
    public void onDecline() {
        getPresenter().onDecline();
    }

    @Override
    public void onText() {
        getPresenter().onText();
    }

    /**
     * OnItemClickListener for the "Respond via SMS" popup.
     */
    public class RespondViaSmsItemClickListener implements AdapterView.OnItemClickListener {
        /**
         * Handles the user selecting an item from the popup.
         */
        @Override
        public void onItemClick(AdapterView<?> parent,  // The ListView
                                View view,  // The TextView that was clicked
                                int position,
                                long id) {
            Logger.d(this, "RespondViaSmsItemClickListener.onItemClick(" + position + ")...");
            final String message = (String) parent.getItemAtPosition(position);
            Logger.v(this, "- message: '" + message + "'");

            // The "Custom" choice is a special case.
            // (For now, it's guaranteed to be the last item.)
            if (position == (parent.getCount() - 1)) {
                // Take the user to the standard SMS compose UI.
                getPresenter().rejectCallWithMessage(null);
            } else {
                getPresenter().rejectCallWithMessage(message);
            }
        }
    }
}