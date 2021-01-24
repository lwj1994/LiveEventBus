package com.lwjlol.liveeventbus.demo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore

/**
 * ViewModel 的工具类
 */
object ViewModelUtil {

    /**
     * 获取一个 ViewModel
     */
    @JvmOverloads
    fun <T : ViewModel> getViewModel(
        store: ViewModelStore,
        viewModelClass: Class<T>,
        factory: ViewModelProvider.Factory? = null,
        key: String? = null
    ): T {
        val f = factory ?: ViewModelProvider.NewInstanceFactory()
        return if (key == null) {
            ViewModelProvider(store, f).get(viewModelClass)
        } else {
            ViewModelProvider(store, f).get(key, viewModelClass)
        }
    }

    /**
     * 获取一个 ViewModel
     */
    @JvmOverloads
    fun <T : ViewModel> getViewModel(
        fragment: Fragment,
        viewModelClass: Class<T>,
        factory: ViewModelProvider.Factory? = null,
        key: String? = null
    ): T {
        return getViewModel(fragment.viewModelStore, viewModelClass, factory, key)
    }

    /**
     * 获取一个 ViewModel
     */
    @JvmOverloads
    fun <T : ViewModel> getViewModel(
        activity: FragmentActivity,
        viewModelClass: Class<T>,
        factory: ViewModelProvider.Factory? = null,
        key: String? = null
    ): T {
        return getViewModel(activity.viewModelStore, viewModelClass, factory, key)
    }

    /**
     * 获取父 Fragment 的 ViewModel
     * @param fragment child fragment
     */
    @JvmOverloads
    fun <T : ViewModel> getParentFragmentViewModel(
        fragment: Fragment,
        viewModelClass: Class<T>,
        factory: ViewModelProvider.Factory,
        key: String? = null
    ): T {
        val parent =
            fragment.parentFragment
                ?: throw IllegalAccessException("${fragment::class.qualifiedName} have no a parent Fragment")
        return getViewModel(parent.viewModelStore, viewModelClass, factory, key)
    }

    /**
     * 获取一个 activity scope 的 ViewModel
     * @param fragment child fragment

     */
    @JvmOverloads
    fun <T : ViewModel> getActivityViewModel(
        fragment: Fragment,
        viewModelClass: Class<T>,
        factory: ViewModelProvider.Factory? = null,
        key: String? = null
    ): T {
        return getViewModel(fragment.requireActivity().viewModelStore, viewModelClass, factory, key)
    }
}
