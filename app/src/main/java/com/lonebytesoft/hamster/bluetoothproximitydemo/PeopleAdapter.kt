package com.lonebytesoft.hamster.bluetoothproximitydemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso

class PeopleAdapter(
    private val dataset: Array<PersonInformation>,
    private val context: Context
) : RecyclerView.Adapter<PeopleAdapter.PersonViewHolder>() {

    class PersonViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PeopleAdapter.PersonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_people, parent, false)
        return PersonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val information = dataset[position]

        Picasso.get()
            .load(information.picture)
            .resizeDimen(R.dimen.item_people_image_width, R.dimen.item_people_image_height)
            .centerInside()
            .into(holder.view.findViewById<ImageView>(R.id.item_people_image))

        holder.view.findViewById<TextView>(R.id.item_people_text).text = information.name

        holder.view.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/${information.id}"))
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = dataset.size

}
