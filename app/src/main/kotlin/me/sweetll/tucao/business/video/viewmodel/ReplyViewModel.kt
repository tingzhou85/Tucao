package me.sweetll.tucao.business.video.viewmodel

import android.content.Intent
import android.databinding.ObservableField
import android.os.Build
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import android.view.View
import com.squareup.moshi.JsonDataException
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.sweetll.tucao.Const
import me.sweetll.tucao.R
import me.sweetll.tucao.base.BaseViewModel
import me.sweetll.tucao.business.login.LoginActivity
import me.sweetll.tucao.business.video.ReplyActivity
import me.sweetll.tucao.di.service.ApiConfig
import me.sweetll.tucao.extension.sanitizeHtml
import me.sweetll.tucao.extension.toast
import me.sweetll.tucao.transition.FabTransform

class ReplyViewModel(val activity: ReplyActivity, val commentId: String, val replyId: String): BaseViewModel() {

    val pageSize = 10
    var pageIndex = 1

    val content = ObservableField<String>("")

    init {
        loadData()
    }

    fun onClickReplyFab(view: View) {
        if (user.isValid()) {
            activity.startFabTransform()
        } else {
            activity.requestLogin()
        }
    }

    fun onClickSendReply(view: View) {

    }

    fun loadData() {
        pageIndex = 0
        jsonApiService.reply(commentId, replyId, pageIndex, pageSize)
                .bindToLifecycle(activity)
                .retryWhen(ApiConfig.RetryWithDelay())
                .subscribeOn(Schedulers.io())
                .map {
                    response ->
                    response.data.map { it.value }.toMutableList()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    data ->
                    pageIndex++
                    activity.loadData(data)
                }, {
                    error ->
                    error.printStackTrace()
                    if (error !is JsonDataException) {
                        error.message?.toast()
                    }
                })
    }

    fun loadMoreData() {
        jsonApiService.reply(commentId, replyId, pageIndex, pageSize)
                .bindToLifecycle(activity)
                .retryWhen(ApiConfig.RetryWithDelay())
                .subscribeOn(Schedulers.io())
                .map {
                    response ->
                    response.data.map { it.value }.toMutableList()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    data ->
                    if (data.size < pageSize) {
                        activity.loadMoreData(data, Const.LOAD_MORE_END)
                    } else {
                        pageIndex++
                        activity.loadMoreData(data, Const.LOAD_MORE_COMPLETE)
                    }
                }, {
                    error ->
                    error.printStackTrace()
                    error.message?.toast()
                    activity.loadMoreData(null, Const.LOAD_MORE_FAIL)
                })
    }

    fun support(id: String) {
                    rawApiService.support(commentId, id)
                            .sanitizeHtml {
                                Object()
                            }
                            .subscribe({

                            }, {
                                error ->
                                error.printStackTrace()
                            })
    }
}

