package com.navigine.navigine.demo.ui.custom.navigation

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class SavedBottomNavigationView : BottomNavigationView {
    private var firstFragmentGraphId = 0
    private var selectedItemTag: String? = null
    private var firstFragmentTag: String? = null
    private var isOnFirstFragment = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private fun setupItemReselected(
        graphIdToTagMap: SparseArray<String>,
        fragmentManager: FragmentManager
    ) {
        this.setOnNavigationItemReselectedListener(object : OnNavigationItemReselectedListener {
            override fun onNavigationItemReselected(item: MenuItem) {
                val newlySelectedItemTag = graphIdToTagMap.get(item.getItemId())
                val selectedFragment =
                    fragmentManager.findFragmentByTag(newlySelectedItemTag) as NavHostFragment?
                val navController = selectedFragment!!.navController
                navController.popBackStack(navController.graph.getStartDestination(), false)
            }
        })
    }

    fun setupWithNavController(
        navGraphIds: MutableList<Int>,
        fragmentManager: FragmentManager,
        containerId: Int, intent: Intent?
    ): LiveData<NavController?> {
        val self: BottomNavigationView = this

        // Map of tags
        val graphIdToTagMap = SparseArray<String>()
        // Result. Mutable live data with the selected controlled
        val selectedNavController = MutableLiveData<NavController?>()

        // First create a NavHostFragment for each NavGraph ID
        var index = 0

        for (navGraphId in navGraphIds) {
            val fragmentTag = getFragmentTag(index)

            // Find or create the Navigation host fragment
            val navHostFragment = obtainNavHostFragment(
                fragmentManager,
                fragmentTag,
                navGraphId,
                containerId
            )

            // Obtain its id
            val graphId = navHostFragment.navController.graph.id
            if (index == 0) {
                firstFragmentGraphId = graphId
            }

            // Save to the map
            graphIdToTagMap.put(graphId, fragmentTag)

            // Attach or detach nav host fragment depending on whether it's the selected item.
            if (this.getSelectedItemId() == graphId) {
                // Update livedata with the selected graph
                selectedNavController.setValue(navHostFragment.navController)
                attachNavHostFragment(fragmentManager, navHostFragment, index == 0)
            } else {
                detachNavHostFragment(fragmentManager, navHostFragment)
            }

            index++
        }

        // Now connect selecting an item with swapping Fragments
        selectedItemTag = graphIdToTagMap.get(this.getSelectedItemId())
        firstFragmentTag = graphIdToTagMap.get(firstFragmentGraphId)
        isOnFirstFragment = selectedItemTag == firstFragmentTag

        setOnNavigationItemSelectedListener(object : OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                // Don't do anything if the state is state has already been saved.
                if (fragmentManager.isStateSaved()) {
                    return false
                } else {
                    val newlySelectedItemTag = graphIdToTagMap.get(item.getItemId())
                    if (selectedItemTag != newlySelectedItemTag) {
                        // Pop everything above the first fragment (the "fixed start destination")
                        fragmentManager.popBackStack(
                            firstFragmentTag,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        val selectedFragment =
                            fragmentManager.findFragmentByTag(newlySelectedItemTag) as NavHostFragment?
                        // Exclude the first fragment tag because it's always in the back stack.
                        if (firstFragmentTag != newlySelectedItemTag) {
                            // Commit a transaction that cleans the back stack and adds the first fragment
                            // to it, creating the fixed started destination.
                            val transaction = fragmentManager.beginTransaction()
                                .show(selectedFragment!!)
                                .setPrimaryNavigationFragment(selectedFragment)


                            for (i in 0..<graphIdToTagMap.size()) {
                                val key = graphIdToTagMap.keyAt(i)
                                // get the object by the key.
                                val tag = graphIdToTagMap.get(key)
                                if (tag != newlySelectedItemTag) {
                                    transaction.hide(
                                        fragmentManager.findFragmentByTag(
                                            firstFragmentTag
                                        )!!
                                    )
                                }
                            }

                            transaction.addToBackStack(firstFragmentTag)
                                .setReorderingAllowed(true)
                                .commit()
                        }

                        selectedItemTag = newlySelectedItemTag
                        isOnFirstFragment = selectedItemTag == firstFragmentTag
                        selectedNavController.setValue(selectedFragment!!.navController)
                        return true
                    } else {
                        return false
                    }
                }
            }
        })

        // Optional: on item reselected, pop back stack to the destination of the graph
        setupItemReselected(graphIdToTagMap, fragmentManager)

        // Handle deep link
        setupDeepLinks(navGraphIds, fragmentManager, containerId, intent)

        // Finally, ensure that we update our BottomNavigationView when the back stack changes
        fragmentManager.addOnBackStackChangedListener(object :
            FragmentManager.OnBackStackChangedListener {
            override fun onBackStackChanged() {
                if (!isOnFirstFragment && !isOnBackStack(fragmentManager, firstFragmentTag)) {
                    self.setSelectedItemId(firstFragmentGraphId)
                }

                // Reset the graph if the currentDestination is not valid (happens when the back
                // stack is popped after using the back button).
                val controller = selectedNavController.getValue()
                if (controller != null) {
                    if (controller.currentDestination == null) {
                        controller.navigate(controller.graph.id)
                    }
                }
            }
        })
        return selectedNavController
    }

    private fun setupDeepLinks(
        navGraphIds: MutableList<Int>,
        fragmentManager: FragmentManager,
        containerId: Int,
        intent: Intent?
    ) {
        var index = 0
        for (navGraphId in navGraphIds) {
            val fragmentTag = getFragmentTag(index)

            // Find or create the Navigation host fragment
            val navHostFragment = obtainNavHostFragment(
                fragmentManager, fragmentTag, navGraphId, containerId
            )

            // Handle Intent
            val graphId = navHostFragment.navController.graph.id
            if (navHostFragment.navController.handleDeepLink(intent)
                && this.getSelectedItemId() != graphId
            ) {
                this.setSelectedItemId(graphId)
            }

            index++
        }
    }

    private fun detachNavHostFragment(
        fragmentManager: FragmentManager,
        navHostFragment: NavHostFragment
    ) {
        fragmentManager.beginTransaction()
            .hide(navHostFragment)
            .commitNow()
    }

    private fun attachNavHostFragment(
        fragmentManager: FragmentManager,
        navHostFragment: NavHostFragment,
        isPrimaryNavFragment: Boolean
    ) {
        val transaction = fragmentManager.beginTransaction()
            .show(navHostFragment)

        if (isPrimaryNavFragment) {
            transaction.setPrimaryNavigationFragment(navHostFragment)
        }

        transaction.commitNow()
    }

    private fun obtainNavHostFragment(
        fragmentManager: FragmentManager,
        fragmentTag: String?,
        navGraphId: Int,
        containerId: Int
    ): NavHostFragment {
        // If the Nav Host fragment exists, return it
        val fragment = fragmentManager.findFragmentByTag(fragmentTag)
        if (fragment != null && fragment is NavHostFragment) {
            return fragment
        }

        // Otherwise, create it and return it.
        val navHostFragment = NavHostFragment.create(navGraphId)
        fragmentManager.beginTransaction()
            .add(containerId, navHostFragment, fragmentTag)
            .commitNow()
        return navHostFragment
    }


    private fun isOnBackStack(fragmentManager: FragmentManager, backStackName: String?): Boolean {
        val backStackCount = fragmentManager.getBackStackEntryCount()
        for (i in 0..<backStackCount) {
            val entry = fragmentManager.getBackStackEntryAt(i)
            if (entry.getName() == backStackName) {
                return true
            }
        }
        return false
    }

    fun getFragmentTag(index: Int): String {
        return "bottomNavigation#" + index
    }
}