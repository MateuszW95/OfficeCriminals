package com.bignerdranch.android.criminalintentkotlin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.ShareCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import kotlinx.android.synthetic.main.fragment_crime.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.Inflater

/**
 * Created by mateusz on 08.02.18.
 */
class CrimeFragment:Fragment() {
    var mCrime: Crime? =null
    var mDateButton:Button?=null
    var mSolvedCheckBox:CheckBox?=null
    var mTitleField:EditText?=null

    val DATE_PICER_TAG:String="Date"
    var mDeleteButton:Button?=null
    var mReportButton:Button?=null
    var mSuspectButton:Button?=null
    var mCallSuspectButton:Button?=null
    lateinit var mPhotoButton:ImageButton
    lateinit var mPhotoView:ImageView
    lateinit var mPhotoFile:File
    val DISPLAY_IMAGE_DIALOG="display_image_dialog"
    private var mCallback:Callbacks?=null


    companion object {
        private val ARG_CRIME_ID="crime_id"
        private val REQUEST_CODE:Int=0
        private val REQUEST_CONTACT=1
        private val REQUEST_CONTACT_PERMISSION=88
        private val REQUEST_PHOTO=2

        val KEY_ID:String="!@####"
        fun newInstance(crimeId:UUID):Fragment{
            var args:Bundle= Bundle()
            args.putSerializable(ARG_CRIME_ID,crimeId)
            var fragment:CrimeFragment= CrimeFragment()
            fragment.arguments=args
            return fragment
        }
        public fun getDataFromIntent(intent: Intent):UUID
        {
            return intent.getSerializableExtra(KEY_ID) as UUID
        }
    }

    interface Callbacks{
        fun onCrimeUpdated(crime: Crime)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mCallback=context as Callbacks
    }

    override fun onDetach() {
        super.onDetach()
        mCallback=null
    }
     fun updateCrime() {
        CrimeLab.get(context).updateCrime(mCrime!!)
        mCallback!!.onCrimeUpdated(mCrime!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var crimeId:UUID=arguments.getSerializable(ARG_CRIME_ID) as UUID
        var cc:CrimeLab= CrimeLab.get(activity)
        mCrime= cc.getCrime(crimeId)
    }

    override fun onPause() {
        super.onPause()
        CrimeLab.get(activity).updateCrime(mCrime!!)
    }
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var v:View?= inflater?.inflate(R.layout.fragment_crime,container,false) ?:null
        mTitleField=v?.findViewById(R.id.crime_title)
        mDateButton=v?.findViewById(R.id.crime_date)
        mSolvedCheckBox=v?.findViewById(R.id.crime_solved)
        mTitleField!!.setText(mCrime!!.mTitle,TextView.BufferType.EDITABLE)
        mDeleteButton=v!!.findViewById(R.id.bt_deleteCrime)
        mCallSuspectButton=v!!.findViewById(R.id.bt_call_suspect)
        mReportButton=v!!.findViewById(R.id.crime_report)
        mSuspectButton=v!!.findViewById(R.id.crime_suspect)
        updateDate()
        mPhotoFile=CrimeLab.get(context).getPhotoFile(mCrime!!)
        mDateButton!!.setOnClickListener(View.OnClickListener {

            var manager:FragmentManager=fragmentManager
            var fragment=DatePickerFragment.newInstance(mCrime!!.mDate)
            fragment.setTargetFragment(this,REQUEST_CODE)
            fragment.show(manager,DATE_PICER_TAG)
        })
        mDeleteButton!!.setOnClickListener(View.OnClickListener {
            var intent:Intent= Intent()
            intent.putExtra(KEY_ID,mCrime!!.mId)
            activity.setResult(Activity.RESULT_OK,intent)
            activity.finish()
        })
        mSolvedCheckBox!!.isChecked=mCrime!!.mSolved
        mTitleField?.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mCrime?.mTitle=s.toString()
                updateCrime()
            }
        })

        mCallSuspectButton!!.setOnClickListener(View.OnClickListener {
            var i=Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+mCrime!!.mSuspectNumber))
            startActivity(i)
        })
        mReportButton!!.setOnClickListener(View.OnClickListener {

            var i=ShareCompat.IntentBuilder.from(activity)
                    .setType("text/plain")
                    .setText(getCrimeReport())
                    .setSubject(getString(R.string.crime_report_subject))
                    .setChooserTitle(R.string.sent_report)
                    .createChooserIntent()
            startActivity(i)
        })

        val pickContact=Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI)

        mSuspectButton!!.setOnClickListener(View.OnClickListener {

            if(ContextCompat.checkSelfPermission(context,android.Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_DENIED){
                requestPermissions(arrayOf( Manifest.permission.READ_CONTACTS), REQUEST_CONTACT_PERMISSION)
            }
            else
            startActivityForResult(pickContact, REQUEST_CONTACT)
        })
        if(mCrime!!.mSuspect!=null) mSuspectButton!!.text=mCrime!!.mSuspect
        mSolvedCheckBox?.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                mCrime?.mSolved=isChecked
                updateCrime()
            }

        })
        if(activity.packageManager.resolveActivity(pickContact,PackageManager.MATCH_DEFAULT_ONLY)==null){
            mSuspectButton!!.isEnabled=false
        }
        if(mCrime!!.mSuspectNumber==null){
            mCallSuspectButton!!.isEnabled=false
        }

        mPhotoButton=v.findViewById(R.id.crime_camera)
        mPhotoView=v.findViewById(R.id.crime_photo)

        var captureImage=Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val canTakePhoto=mPhotoFile!=null && captureImage.resolveActivity(activity.packageManager)!=null
        mPhotoButton.isEnabled=canTakePhoto
        mPhotoButton.setOnClickListener(View.OnClickListener {
            val uri=FileProvider.getUriForFile(context,"com.bignerdranch.android.criminalintentkotlin.fileprovider",mPhotoFile)
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT,uri)

            val cameraActivities=activity.packageManager.queryIntentActivities(captureImage,PackageManager.MATCH_DEFAULT_ONLY) as List<ResolveInfo>

            for( i in cameraActivities){
                activity.grantUriPermission(i.activityInfo.packageName,uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }

            startActivityForResult(captureImage, REQUEST_PHOTO)
        })

        mPhotoView.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                mPhotoView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                updatePhotoView()
            }
        }
        )

        mPhotoView.setOnClickListener(View.OnClickListener {

            var manager:FragmentManager=fragmentManager
            var f= DisplayImageDialogFragment.newInstance(mPhotoFile.absolutePath)
            f.show(manager,DISPLAY_IMAGE_DIALOG)
        })

        return v
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== REQUEST_CONTACT_PERMISSION){
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(context, "READ_CONTACT permission denied", Toast.LENGTH_SHORT).show()
            } else
                Toast.makeText(context, "READ_CONTACT permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode!= Activity.RESULT_OK) return

        if(requestCode==REQUEST_CODE){

            var date:Date= data!!.getSerializableExtra(DatePickerFragment.EXTRA_DATE) as Date
            mCrime!!.mDate=date
            updateDate()
            updateCrime()

        }else if(requestCode== REQUEST_CONTACT)
        {
            var contactUri:Uri=data!!.data
            var queryFields= arrayOf(ContactsContract.Contacts.DISPLAY_NAME,ContactsContract.Contacts._ID)

            var c=activity.contentResolver.query(contactUri,queryFields,null,null,null)

            try{
                if(c.count==0) return

                c.moveToFirst()
                var suspect:String=c.getString(0)
                var id=c.getString(1)
                c=activity.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                        null,
                        null)
                if(c.count==0) c.close()

                c.moveToFirst()
                mCrime!!.mSuspectNumber=c.getString(0);
                mCrime!!.mSuspect=suspect
                mSuspectButton!!.text=suspect
                updateCrime()
            }
            finally {
                c.close()
            }
        }else if(requestCode==REQUEST_PHOTO){
            var uri=FileProvider.getUriForFile(context,"com.bignerdranch.android.criminalintentkotlin.fileprovider",mPhotoFile)
            activity.revokeUriPermission(uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            updatePhotoView()
            updateCrime()
        }
    }
    fun updateDate(){
        var sdf:SimpleDateFormat=SimpleDateFormat("d/M/YYYY H:m")
        mDateButton!!.text=sdf.format(mCrime!!.mDate)
    }
    fun getCrimeReport():String{
        var solvedString:String?=null
        if(mCrime!!.mSolved){
            solvedString=getString(R.string.crime_report_solved)
        }
        else{
            solvedString=getString(R.string.crime_report_unsolved)
        }

        var dateFormat:String="EEE, MMM dd"
        var dateString:String=DateFormat.format(dateFormat,mCrime!!.mDate).toString()

        var suspect=mCrime!!.mSuspect
        if(suspect==null){
            suspect=getString(R.string.crime_report_no_suspect)
        }
        else
        {
            suspect=getString(R.string.crime_report_suspect,suspect)
        }

        var report:String=getString(R.string.crime_report,mCrime!!.mTitle,dateString,solvedString,suspect)
        return  report
    }

    private fun updatePhotoView(){
        if(mPhotoFile==null || !mPhotoFile.exists()){
            mPhotoView.setImageBitmap(null)
        } else if(mPhotoView!=null){
            mPhotoView.setImageBitmap(PictureUtils.getScaledBitmap(mPhotoFile.path,mPhotoView))
        }
        else{
            mPhotoView.setImageBitmap(PictureUtils.getScaledBitmap(mPhotoFile.path,activity))
        }
    }
    override fun onResume() {
        super.onResume()
        mCallSuspectButton!!.isEnabled = mCrime!!.mSuspectNumber != null

    }


}