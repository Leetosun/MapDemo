package com.leomo.re.map

import android.view.View
import java.util.*

/**
 * 作者：yuanYe创建于2016/12/17
 * QQ：962851730

 * 实现OnClickListener, 重写onClick()方法防止出现连点情况
 * 建议通过内部类方式实现
 */
abstract class OnClickListener : View.OnClickListener {
    // 最后一次点击时间
    private var lastClickTime: Long = 0

    /**
     * 防止出现多次点击
     */
    abstract fun onNoDoubleClick(v: View)

    /**
     * 重写onClick()方法, 限制1 秒内只能点击一次
     */
    override fun onClick(v: View) {
        val currentTime = Calendar.getInstance().timeInMillis
        if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
            lastClickTime = currentTime
            onNoDoubleClick(v)
        }
    }

    companion object {

        // 两次点击事件的时间 间隔
        private val MIN_CLICK_DELAY_TIME = 1000
    }
}
