package com.fastaccess.github.ui.modules.profile.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.fastaccess.data.persistence.models.UserModel
import com.fastaccess.github.R
import com.fastaccess.github.base.BaseFragment
import com.fastaccess.github.platform.glide.GlideApp
import com.fastaccess.github.ui.adapter.ProfileOrganizationCell
import com.fastaccess.github.ui.adapter.ProfilePinnedRepoCell
import com.fastaccess.github.ui.modules.profile.fragment.viewmodel.ProfileViewModel
import com.fastaccess.github.utils.BundleConstant
import com.fastaccess.github.utils.extensions.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.appbar_center_title_layout.*
import kotlinx.android.synthetic.main.profile_bottom_sheet.*
import kotlinx.android.synthetic.main.profile_fragment_layout.*
import javax.inject.Inject

/**
 * Created by Kosh on 18.08.18.
 */
class ProfileFragment : BaseFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel by lazy { ViewModelProviders.of(this, viewModelFactory).get(ProfileViewModel::class.java) }

    private val loginBundle: String by lazy { arguments?.getString(BundleConstant.EXTRA) ?: "" }

    override fun layoutRes(): Int = R.layout.profile_fragment_layout

    override fun onFragmentCreatedWithUser(view: View, savedInstanceState: Bundle?) {
        username.text = loginBundle
        toolbarTitle.text = getString(R.string.profile)
        actionsHolder.isVisible = loginBundle != me()
        toolbar.navigationIcon = getDrawable(R.drawable.ic_back)
        toolbar.setNavigationOnClickListener { activity?.onBackPressed() }

        swipeRefresh.setOnRefreshListener {
            viewModel.getUserFromRemote(loginBundle)
        }

        observeChanges()

        val behaviour = BottomSheetBehavior.from(bottomSheet)
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) = expandBottomSheet()
            override fun onTabUnselected(p0: TabLayout.Tab?) {}
            override fun onTabSelected(p0: TabLayout.Tab?) = expandBottomSheet()

            private fun expandBottomSheet() {
                (behaviour.state != BottomSheetBehavior.STATE_EXPANDED).isTrue {
                    behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        })

        tabs.addTab(tabs.newTab().setText(R.string.feeds), false)
        tabs.addTab(tabs.newTab().setText(R.string.feeds), false)
        tabs.addTab(tabs.newTab().setText(R.string.feeds), false)
        tabs.addTab(tabs.newTab().setText(R.string.feeds), false)
        tabs.addTab(tabs.newTab().setText(R.string.feeds), false)

    }

    private fun observeChanges() {
        viewModel.getUser(loginBundle).observeNull(this) { user ->
            if (user == null) {
                viewModel.getUserFromRemote(loginBundle)
            } else {
                initUI(user)
            }
        }

        viewModel.progress.observeNotNull(this) {
            swipeRefresh.isRefreshing = it == true
        }

        viewModel.error.observeNotNull(this) {
            view?.let { view -> showSnackBar(view, resId = it.resId, message = it.message) }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initUI(user: UserModel) {
        following.text = "${getString(R.string.following)}: ${user.following?.totalCount ?: 0}"
        followers.text = "${getString(R.string.followers)}: ${user.followers?.totalCount ?: 0}"
        swipeRefresh.isRefreshing = false
        followBtn.text = if (user.viewerIsFollowing == true) {
            getString(R.string.unfollow)
        } else {
            getString(R.string.follow)
        }
        blockBtn.isVisible = false /*user.isViewer == true*/
        description.isVisible = user.bio?.isNotEmpty() == true
        description.text = user.bio ?: ""
        email.isVisible = user.email?.isNotEmpty() == true
        email.text = user.email
        company.isVisible = user.company?.isNotEmpty() == true
        company.text = user.company ?: ""
        joined.text = user.createdAt?.timeAgo() ?: ""
        joined.isVisible = true
        location.isVisible = user.location?.isNotEmpty() == true
        location.text = user.location ?: ""
        name.isVisible = user.name?.isNotEmpty() == true
        name.text = user.name ?: ""
        developerProgram.isVisible = user.isDeveloperProgramMember == true
        user.organizations?.nodes?.let { orgs ->
            if (orgs.isNotEmpty()) {
                organizationHolder.isVisible = true
                organizationList.removeAllCells()
                organizationList.addCells(orgs.map { ProfileOrganizationCell(it, GlideApp.with(this)) })
            }
        }
        user.pinnedRepositories?.pinnedRepositories?.let { nodes ->
            if (nodes.isNotEmpty()) {
                pinnedHolder.isVisible = true
                pinnedList.addDivider()
                pinnedList.removeAllCells()
                pinnedList.addCells(nodes.map { ProfilePinnedRepoCell(it) })
            }
        }
        GlideApp.with(this)
                .load(user.avatarUrl)
                .fallback(R.drawable.ic_profile)
                .circleCrop()
                .into(userImageView)
    }

    companion object {
        fun newInstance(login: String) = ProfileFragment().apply {
            arguments = Bundle().apply {
                putString(BundleConstant.EXTRA, login)
            }
        }
    }
}