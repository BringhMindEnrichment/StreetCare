package org.brightmindenrichment.street_care.ui.community.adapter

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.card.MaterialCardView
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.StickyHeaderInterface
import org.brightmindenrichment.street_care.ui.community.data.CommunityData
import org.brightmindenrichment.street_care.ui.community.data.Event
import org.brightmindenrichment.street_care.ui.community.data.EventDataAdapter
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Extensions.Companion.createHighlightedSkillTextView
import org.brightmindenrichment.street_care.util.Extensions.Companion.createSkillTextView
import org.brightmindenrichment.street_care.util.Extensions.Companion.refreshNumOfInterest
import org.brightmindenrichment.street_care.util.Extensions.Companion.replaceRSVPButton
import org.brightmindenrichment.street_care.util.Extensions.Companion.setRSVPButton
import org.brightmindenrichment.street_care.util.Extensions.Companion.setVerifiedAndRegistered


class CommunityRecyclerAdapter(
    private val controller: EventDataAdapter,
    private val communityPageName: CommunityPageName,
    //private val isPastEvents: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), StickyHeaderInterface {

    interface ClickListener {
        fun onClick(event: Event, position: Int){}
    }
    private var clickListener:ClickListener?=null

    fun setClickListener(clickListener: ClickListener) {
        this.clickListener = clickListener
    }

    private lateinit var refreshBottomSheet: (Event) -> Unit
    fun setRefreshBottomSheet(
        refreshBottomSheet: (Event) -> Unit
    ){
        this.refreshBottomSheet = refreshBottomSheet
    }


    fun getItemPosition(eventId: String?): Int? {
        for (pos in 0 until controller.size) {
            val communityData = controller.getEventAtPosition(pos)
            if(communityData?.event?.eventId == eventId) return pos
        }
        return null
    }

    fun getItemAtPosition(pos: Int): CommunityData? {
        return controller.getEventAtPosition(pos)
    }

    fun clickItem(event: Event, pos: Int) {
        clickListener!!.onClick(event, pos)
    }

    private var currentHeaderText: String? = null

    fun getCurrentHeaderText(): String? {
        return currentHeaderText
    }


    inner class EventViewHolder(private val communityItemView: View) : RecyclerView.ViewHolder(communityItemView) {

        private val textViewTitle: TextView = communityItemView.findViewById<TextView>(R.id.textViewCommunityTitle)
        private val textViewCommunityLocation: TextView = communityItemView.findViewById<TextView>(R.id.textViewCommunityLocation)
        private val textViewCommunityTime: TextView = communityItemView.findViewById<TextView>(R.id.textViewCommunityTime)
        private val textViewDate: TextView = communityItemView.findViewById<TextView>(R.id.textViewDate)
        private val textViewDay: TextView = communityItemView.findViewById<TextView>(R.id.textViewDay)
        private val buttonRSVP: AppCompatButton = communityItemView.findViewById<AppCompatButton>(R.id.btnRSVP)
        private val relativeLayoutImage: RelativeLayout = communityItemView.findViewById<RelativeLayout>(R.id.relativeLayoutImage)
        private val textInterested:TextView = communityItemView.findViewById<TextView>(R.id.textInterested)
        private val cardViewEvent:MaterialCardView = communityItemView.findViewById<MaterialCardView>(R.id.cardViewEvent)
        private val linearLayoutVerified: LinearLayout = communityItemView.findViewById<LinearLayout>(R.id.llVerifiedAndRegistered)
        private val textHelpType:TextView = communityItemView.findViewById<TextView>(R.id.tvHelpType)
        private val linearLayoutVerifiedAndIcon: LinearLayout = communityItemView.findViewById(R.id.llVerifiedAndIcon)
        private val textViewRegistered: TextView = communityItemView.findViewById(R.id.tvRegistered)
        private val textViewEventStatus: TextView = communityItemView.findViewById(R.id.tvEventStatus)
        private val flSkills: FlexboxLayout = communityItemView.findViewById(R.id.flSkills)

        init {
            cardViewEvent.setOnClickListener{
                val position = bindingAdapterPosition
                clickListener?.let {
                    if(position != RecyclerView.NO_POSITION){
                        val communityData = controller.getEventAtPosition(position)
                        communityData?.event?.let{ event ->
                            clickListener!!.onClick(event, position)
                        }
                    }
                }
            }

            buttonRSVP.setOnClickListener {

                val position = bindingAdapterPosition
                val communityData = controller.getEventAtPosition(position)
                if(communityData!=null){
                    communityData.event?.let{ event->
                        //val isFavorite = event.liked
                        event.signedUp=!event.signedUp

//                        if(isFavorite){
//                            imageViewUnFav.setImageResource(R.drawable.ic_unfav)
//                            //event.interest = event.interest?.minus(1)
//
//                        }
//                        else{
//                            imageViewUnFav.setImageResource(R.drawable.ic_favorite)
//                            //event.interest = event.interest?.plus(1)
//                        }

                        // refreshNumOfInterestAndProfileImg(event)
                        // notifyDataSetChanged()
                        controller.setLikedEvent(event) {
                            refreshBottomSheet(it)
                            notifyItemChanged(position)
                            Log.d("Liked Event Firebase Update", "Liked Event Firebase Update Success")
                        }
                    }

                }
            }

        }

        fun bind(pos: Int, context: Context) {
            // Bind data to views
            // ...
            val communityData = controller.getEventAtPosition(pos)
            communityData?.event?.let{ event->
                textViewTitle.text = event.title
                textViewCommunityLocation.text = event.location.orEmpty()
                textViewCommunityTime.text = event.time.orEmpty()
                textViewDate.text = event.date.orEmpty()
                textViewDay.text = event.day.orEmpty()

                event.skills?.let { skills ->
                    flSkills.removeAllViews()
                    for(skill in skills) {
                        flSkills.addView(createSkillTextView(skill, context))
                    }
                }

                Log.d("query", "event.interest: ${event.interest}")
                Log.d("query", "event.itemList.size: ${event.itemList.size}")

                val approved = event.approved!!
                val isSignedUp = event.signedUp
                // numOfInterest = event.interest?.minus(event.itemList.size)
                when(communityPageName) {
                    CommunityPageName.PAST_EVENTS -> {
                        if(!event.signedUp) {
                            replaceRSVPButton(
                                buttonRSVP = buttonRSVP,
                                tvEventStatus = textViewEventStatus,
                                textId = R.string.completed,
                            )
                            /*
                            buttonRSVP.setText(R.string.completed)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                            buttonRSVP.isEnabled = false
                             */
                        }
                        else {
                            replaceRSVPButton(
                                buttonRSVP = buttonRSVP,
                                tvEventStatus = textViewEventStatus,
                                textId = R.string.attended,
                            )
                            /*
                            buttonRSVP.setText(R.string.attended)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                            buttonRSVP.isEnabled = false
                             */
                        }
                    }
                    CommunityPageName.UPCOMING_EVENTS, CommunityPageName.HELP_REQUESTS -> {
                        if (isSignedUp) {
                            setRSVPButton(
                                buttonRSVP = buttonRSVP,
                                textId = R.string.deregister,
                                textColor = Color.BLACK,
                                backgroundColor = null
                            )
                            /*
                            buttonRSVP.setText(R.string.unregister)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                             */
                        } else {
                            if(event.totalSlots == null || event.totalSlots == -1 || (event.participants?.size ?: 0) < event.totalSlots!!) {
                                setRSVPButton(
                                    buttonRSVP = buttonRSVP,
                                    textId = R.string.rsvp,
                                    textColor = Color.parseColor("#FEED00"),
                                    backgroundColor = Color.parseColor("#002925")
                                )
                                /*
                                val color = Color.parseColor("#FEED00")
                                buttonRSVP.setText(R.string.rsvp)
                                buttonRSVP.backgroundTintList = ColorStateList.valueOf(
                                    Color.parseColor("#002925")
                                )
                                buttonRSVP.setTextColor(color)
                                 */
                            }
                            else {
                                replaceRSVPButton(
                                    buttonRSVP = buttonRSVP,
                                    tvEventStatus = textViewEventStatus,
                                    textId = R.string.event_full,
                                )
                                /*
                                buttonRSVP.setText(R.string.event_full)
                                buttonRSVP.backgroundTintList = null
                                buttonRSVP.setTextColor(Color.BLACK)
                                buttonRSVP.isEnabled = false
                                 */
                            }
                        }
                    }
                }
                /*
                if(!isPastEvents) {
                    if (isSignedUp) {
                        setRSVPButton(
                            buttonRSVP = buttonRSVP,
                            textId = R.string.deregister,
                            textColor = Color.BLACK,
                            backgroundColor = null
                        )
                        /*
                        buttonRSVP.setText(R.string.unregister)
                        buttonRSVP.backgroundTintList = null
                        buttonRSVP.setTextColor(Color.BLACK)
                         */
                    } else {
                        if(event.totalSlots == null || event.totalSlots == -1 || (event.participants?.size ?: 0) < event.totalSlots!!) {
                            setRSVPButton(
                                buttonRSVP = buttonRSVP,
                                textId = R.string.rsvp,
                                textColor = Color.parseColor("#FEED00"),
                                backgroundColor = Color.parseColor("#002925")
                            )
                            /*
                            val color = Color.parseColor("#FEED00")
                            buttonRSVP.setText(R.string.rsvp)
                            buttonRSVP.backgroundTintList = ColorStateList.valueOf(
                                Color.parseColor("#002925")
                            )
                            buttonRSVP.setTextColor(color)
                             */
                        }
                        else {
                            replaceRSVPButton(
                                buttonRSVP = buttonRSVP,
                                tvEventStatus = textViewEventStatus,
                                textId = R.string.event_full,
                            )
                            /*
                            buttonRSVP.setText(R.string.event_full)
                            buttonRSVP.backgroundTintList = null
                            buttonRSVP.setTextColor(Color.BLACK)
                            buttonRSVP.isEnabled = false
                             */
                        }
                    }
                }
                else {
                    if(!event.signedUp) {
                        replaceRSVPButton(
                            buttonRSVP = buttonRSVP,
                            tvEventStatus = textViewEventStatus,
                            textId = R.string.completed,
                        )
                        /*
                        buttonRSVP.setText(R.string.completed)
                        buttonRSVP.backgroundTintList = null
                        buttonRSVP.setTextColor(Color.BLACK)
                        buttonRSVP.isEnabled = false
                         */
                    }
                    else {
                        replaceRSVPButton(
                            buttonRSVP = buttonRSVP,
                            tvEventStatus = textViewEventStatus,
                            textId = R.string.attended,
                        )
                        /*
                        buttonRSVP.setText(R.string.attended)
                        buttonRSVP.backgroundTintList = null
                        buttonRSVP.setTextColor(Color.BLACK)
                        buttonRSVP.isEnabled = false
                         */
                    }
                }
                 */

                Log.d("syncWebApp", "approved: $approved")
                val isPastEvents = communityPageName == CommunityPageName.PAST_EVENTS
                setVerifiedAndRegistered(
                    context = null,
                    isVerified = approved,
                    isRegistered = isSignedUp,
                    isEventCard = true,
                    linearLayoutVerified = linearLayoutVerified,
                    linearLayoutVerifiedAndIcon = linearLayoutVerifiedAndIcon,
                    textViewRegistered = textViewRegistered,
                    cardViewEvent = cardViewEvent,
                    bottomSheetView = null,
                    isPastEvents = isPastEvents,
                )
                /*
                if(approved) {
                    linearLayoutVerified.visibility = View.VISIBLE
                    cardViewEvent.strokeWidth = (1.5).toPx()
                    //cardViewEvent.strokeColor = Color.parseColor("#007AFF")
                }
                else {
                    linearLayoutVerified.visibility = View.GONE
                    //cardViewEvent.strokeWidth = 0
                }

                 */

                textHelpType.text = event.helpType?: "Help Type Required"


                when(event.layoutType){
                    Extensions.TYPE_DAY ->{
                        textViewDate.visibility = View.INVISIBLE
                        textViewDay.visibility = View.INVISIBLE
                    }

                }

                //refreshNumOfInterestAndProfileImg(event, textInterested, relativeLayoutImage)

                // refreshNumOfInterest
                refreshNumOfInterest(event, textInterested, isPastEvents, context)
            }
        }

    }

    inner class YearViewHolder (yearItemView: View) : RecyclerView.ViewHolder(yearItemView){
        private val textViewYear: TextView = yearItemView.findViewById<TextView>(R.id.textViewCommunityYear)
        fun bind(pos: Int) {
            // Bind data to views
            // ...
            val communityData = controller.getEventAtPosition(pos)
            communityData?.eventYear?.let{ eventYear->
                textViewYear.text = eventYear.year
                currentHeaderText = eventYear.year
            }
            //Log.d("stickyHeader", "pos: $pos, textViewYear: ${textViewYear.text}")
        }

    }





    override  fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RecyclerView.ViewHolder {

        return if(viewType==Extensions.TYPE_MONTH){
            val view = LayoutInflater.from(parent.context).inflate(R.layout.card_community_year, parent, false)
            YearViewHolder(view)
        } else{
            val view = LayoutInflater.from(parent.context).inflate(R.layout.event_list_layout, parent, false)
            EventViewHolder(view)
        }


    }
    override fun getItemCount(): Int {
        return controller.size
    }

    override fun getItemViewType(position: Int): Int {
        val communityData = controller.getEventAtPosition(position)
        return communityData?.layoutType ?:0
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EventViewHolder -> {
                holder.bind(position, holder.itemView.context)
            }
            is YearViewHolder -> {
                holder.bind(position)
            }
        }
    }

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var headerPosition = 0
        var theItemPosition = itemPosition
        do {
            if (isHeader(theItemPosition)) {
                headerPosition = theItemPosition
                break
            }
            theItemPosition -= 1
        } while (theItemPosition >= 0)
        return headerPosition
    }

    override fun getHeaderLayout(headerPosition: Int): Int {
        return R.layout.card_community_year
    }

    override fun bindHeaderData(header: View?, headerPosition: Int) {
        header?.let{
            it.findViewById<TextView>(R.id.textViewCommunityYear).text = controller.getEventAtPosition(headerPosition)?.eventYear?.year
            it.findViewById<TextView>(R.id.textViewCommunityYear).setBackgroundColor(Color.WHITE)
        }
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return controller.getEventAtPosition(itemPosition)?.layoutType == Extensions.TYPE_MONTH
    }

}
