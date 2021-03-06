package com.intellisoft.nndak.holders

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.intellisoft.nndak.charts.ItemOrder
import com.intellisoft.nndak.databinding.OrderListViewBinding
import com.intellisoft.nndak.models.OrdersItem
import timber.log.Timber

class OrdersItemViewHolder(binding: OrderListViewBinding) :
    RecyclerView.ViewHolder(binding.root) {
    private val lnParent: LinearLayout = binding.lnParent
    private val appBabyName: TextView = binding.appBabyName
    private val appMotherName: TextView = binding.appMotherName
    private val appIpNumber: TextView = binding.appIpNumber
    private val appBabyAge: TextView = binding.appBabyAge
    private val appDhmType: TextView = binding.appDhmType
    private val appConsent: TextView = binding.appConsent
    private val appAction: TextView = binding.appAction
    private val lnAction: LinearLayout = binding.lnAction

    fun bindTo(
        ordersItem: ItemOrder,
        onItemClicked: (ItemOrder) -> Unit
    ) {
        this.appBabyName.text = ordersItem.babyName
        this.appMotherName.text = ordersItem.motherName
        this.appIpNumber.text = ordersItem.motherIp
        this.appBabyAge.text = ordersItem.babyAge
        this.appDhmType.text = ordersItem.dhmType
        this.appConsent.text = ordersItem.consentGiven

        /**
         * Hide Action Textview and Show Linear
         */
        this.appAction.visibility = View.GONE
        this.lnAction.visibility = View.VISIBLE

        this.lnAction.setOnClickListener {
            onItemClicked(ordersItem)
        }

    }
}