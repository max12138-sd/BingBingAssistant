package com.bingbing.assistant.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bingbing.assistant.R
import com.bingbing.assistant.core.AppGraph
import com.bingbing.assistant.data.entity.HeroEntity
import com.bingbing.assistant.data.entity.MyHeroEntity
import com.bingbing.assistant.databinding.ActivityMyPoolBinding
import com.bingbing.assistant.databinding.ItemHeroBinding
import com.bingbing.assistant.feature.pool.MyPoolFlow
import kotlinx.coroutines.launch

/**
 * 我的英雄库（§7.2）：网格列表 + 勾选拥有 + 长按星级 + 一键全选 5 星。
 */
class MyPoolActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyPoolBinding
    private val flow = MyPoolFlow()
    private val adapter = HeroAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPoolBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerHeroes.layoutManager = LinearLayoutManager(this)
        binding.recyclerHeroes.adapter = adapter

        binding.btnSelectAll5.setOnClickListener {
            lifecycleScope.launch {
                val n = flow.selectAllFiveStar()
                Toast.makeText(this@MyPoolActivity, "已全选 $n 名英雄为 5 星", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.observe().collect { state ->
                    adapter.submit(state)
                }
            }
        }
    }

    private inner class HeroAdapter : RecyclerView.Adapter<HeroVH>() {

        private val items = mutableListOf<HeroEntity>()
        private val owned = mutableMapOf<String, MyHeroEntity>()

        fun submit(state: MyPoolFlow.PoolUiState) {
            items.clear()
            items.addAll(state.heroes)
            owned.clear()
            owned.putAll(state.owned)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeroVH =
            HeroVH(ItemHeroBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: HeroVH, position: Int) {
            val hero = items[position]
            val mine = owned[hero.id]
            holder.bind(hero, mine, flow)
        }
    }

    private inner class HeroVH(
        private val item: ItemHeroBinding,
    ) : RecyclerView.ViewHolder(item.root) {

        private var hero: HeroEntity? = null
        private var mine: MyHeroEntity? = null

        init {
            item.cbOwned.setOnClickListener {
                val h = hero ?: return@setOnClickListener
                lifecycleScope.launch { flow.toggle(h.id, mine != null) }
            }
            item.root.setOnLongClickListener {
                val h = hero ?: return@setOnLongClickListener true
                if (mine == null) {
                    Toast.makeText(this@MyPoolActivity, "先勾选拥有，再调星级", Toast.LENGTH_SHORT).show()
                } else {
                    showStarDialog(h)
                }
                true
            }
        }

        fun bind(h: HeroEntity, m: MyHeroEntity?, flow: MyPoolFlow) {
            hero = h
            mine = m
            item.tvHeroName.text = h.name
            item.tvHeroMeta.text = "${roleCn(h.role)} · ${h.faction ?: "无阵营"} · ${h.id}"
            item.tvStar.text = m?.let { getString(R.string.pool_star_fmt, it.star) } ?: ""
            item.cbOwned.isChecked = m != null
        }

        private fun showStarDialog(h: HeroEntity) {
            val stars = arrayOf("3 星", "4 星", "5 星")
            AlertDialog.Builder(this@MyPoolActivity)
                .setTitle("${h.name} 的星级")
                .setItems(stars) { _, which ->
                    lifecycleScope.launch { flow.setStar(h.id, which + 3) }
                }
                .show()
        }
    }

    private fun roleCn(role: String) = when (role) {
        "FRONT" -> "前排"
        "MID" -> "中排"
        "BACK" -> "后排"
        else -> role
    }
}
