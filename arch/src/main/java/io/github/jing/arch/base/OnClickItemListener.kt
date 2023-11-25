package io.github.jing.arch.base

import android.view.View

interface OnClickItemListener<T> {
    fun onClick(view: View, data: T)
}