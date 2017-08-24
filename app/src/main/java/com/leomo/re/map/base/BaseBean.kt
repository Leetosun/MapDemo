package com.leomo.re.map.base

import com.google.gson.annotations.SerializedName

/**
 * 作者：yuanYe创建于2016/11/14
 * QQ：962851730

 * 服务端接口返回实体基本类, 对应后端制定的json串格式
 */
class BaseBean {

    // 状态码
    var code: String? = null

    // 信息
    @SerializedName("msg")
    var message: String? = null

    // 具体数据内容(json)
    var data: String? = null

    override fun toString(): String {
        return StringBuilder("BaseBean{").append("code='").append(code).append('\'').append(", message='")
                .append(message).append('\'').append(", data='").append(data).append('\'').append('}').toString()
    }
}
