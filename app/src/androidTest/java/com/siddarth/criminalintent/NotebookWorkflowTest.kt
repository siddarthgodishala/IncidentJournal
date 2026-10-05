package com.siddarth.criminalintent

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.intent.IntentCallback
import androidx.test.runner.intent.IntentMonitorRegistry
import androidx.recyclerview.widget.RecyclerView
import com.siddarth.criminalintent.storage.Incident
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.containsString
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class NotebookWorkflowTest {
    private val app = ApplicationProvider.getApplicationContext<NotebookApplication>()
    private val dao get() = app.store.incidents()
    private lateinit var screen: ActivityScenario<NotebookActivity>
    @Before fun start() {
        runBlocking { dao.watch().first().forEach { dao.remove(it.key) } }
        app.evidenceDirectory.listFiles()?.forEach { it.delete() }
        Intents.init()
        screen = ActivityScenario.launch(NotebookActivity::class.java)
        ready(R.id.new_entry)
    }
    @After fun finish() {
        screen.close()
        Intents.release()
        runBlocking { dao.watch().first().forEach { dao.remove(it.key) } }
        app.evidenceDirectory.listFiles()?.forEach { it.delete() }
    }
    @Test fun draftRotationSaveReopenAndDelete() {
        onView(withId(R.id.new_entry)).perform(click())
        ready(R.id.entry_title)
        onView(withId(R.id.action_save)).perform(click())
        assertTrue(runBlocking { dao.watch().first().isEmpty() })
        onView(withId(R.id.entry_title)).perform(replaceText("Printer left without paper"), closeSoftKeyboard())
        onView(withId(R.id.entry_notes)).perform(replaceText("Restocked the lower tray."), closeSoftKeyboard())
        onView(withId(R.id.pick_date)).perform(scrollTo(), click())
        onView(isAssignableFrom(DatePicker::class.java)).perform(PickerActions.setDate(2026, 8, 19))
        onView(withText("OK")).perform(click())
        onView(withId(R.id.pick_time)).perform(scrollTo(), click())
        onView(isAssignableFrom(TimePicker::class.java)).perform(PickerActions.setTime(9, 20))
        onView(withText("OK")).perform(click())
        onView(withId(R.id.entry_solved)).perform(scrollTo(), click())
        screen.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        ready(R.id.entry_title)
        screen.recreate()
        ready(R.id.entry_title)
        onView(withId(R.id.entry_title)).perform(scrollTo()).check(matches(withText("Printer left without paper")))
        onView(withId(R.id.entry_solved)).perform(scrollTo()).check(matches(isChecked()))
        assertTrue(runBlocking { dao.watch().first().isEmpty() })
        Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(ActivityResult(Activity.RESULT_OK,null))
        onView(withId(R.id.send_report)).perform(scrollTo(),click())
        val chooser = Intents.getIntents().last { it.action == Intent.ACTION_CHOOSER }
        @Suppress("DEPRECATION") val report = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, report.action)
        assertTrue(report.getStringExtra(Intent.EXTRA_TEXT)!!.contains("Restocked the lower tray."))
        onView(withId(R.id.action_save)).perform(click())
        ready(R.id.new_entry)
        val record=runBlocking { dao.watch().first().single() }
        assertTrue(record.resolved)
        val date=Calendar.getInstance().apply { timeInMillis=record.occurredAt }
        assertEquals(19,date.get(Calendar.DAY_OF_MONTH)); assertEquals(9,date.get(Calendar.HOUR_OF_DAY)); assertEquals(20,date.get(Calendar.MINUTE))
        screen.close(); screen=ActivityScenario.launch(NotebookActivity::class.java)
        ready(R.id.entries)
        onView(withText(record.heading)).perform(click()); ready(R.id.entry_title)
        onView(withId(R.id.entry_notes)).check(matches(withText(record.notes)))
        openDelete(); onView(withText(R.string.cancel)).perform(click())
        assertNotNull(runBlocking { dao.read(record.key) })
        openDelete(); onView(withText(R.string.delete)).perform(click())
        ready(R.id.new_entry)
        assertNull(runBlocking { dao.read(record.key) })
    }
    @Test fun filtersSearchAndLongList() {
        runBlocking { repeat(36) { dao.write(Incident(key="item-$it", heading="Incident $it", occurredAt=it.toLong(), resolved=it%2==0)) } }
        await { runBlocking { dao.watch().first().size == 36 } }
        ready(R.id.entries)
        onView(withId(R.id.entries)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(35))
        onView(withText("Incident 0")).check(matches(isDisplayed()))
        onView(withId(R.id.filter_open)).perform(click())
        onView(withId(R.id.search)).perform(replaceText("Incident 35"), closeSoftKeyboard())
        awaitRows(1)
        onView(withId(R.id.row_title)).check(matches(withText("Incident 35")))
        onView(withId(R.id.filter_solved)).perform(click()); awaitRows(0)
        onView(withId(R.id.empty_message)).check(matches(withText(R.string.no_matches)))
        screen.recreate(); ready(R.id.search)
        onView(withId(R.id.search)).check(matches(withText("Incident 35")))
        onView(withId(R.id.filter_solved)).check(matches(isChecked()))
        onView(withId(R.id.action_new)).perform(click()); ready(R.id.entry_title)
        onView(withId(R.id.entry_title)).perform(replaceText("Discard this draft"),closeSoftKeyboard())
        pressBack(); onView(withText(R.string.discard)).perform(click()); ready(R.id.new_entry)
        assertEquals(36,runBlocking { dao.watch().first().size })
    }
    @Test fun cameraAttachmentRetakeCancellationAndDiscard() {
        onView(withId(R.id.new_entry)).perform(click()); ready(R.id.entry_title)
        onView(withId(R.id.entry_title)).perform(replaceText("Broken stapler"),closeSoftKeyboard())
        val cameraWriter = IntentCallback { intent ->
            if(intent.action==MediaStore.ACTION_IMAGE_CAPTURE) {
                @Suppress("DEPRECATION") val uri=intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
                app.contentResolver.openOutputStream(uri)!!.use { output ->
                    Bitmap.createBitmap(40,40,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.GREEN); compress(Bitmap.CompressFormat.JPEG,90,output); recycle() }
                }
            }
        }
        IntentMonitorRegistry.getInstance().addIntentCallback(cameraWriter)
        Intents.intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(ActivityResult(Activity.RESULT_OK,null))
        onView(withId(R.id.capture)).perform(scrollTo(),click())
        await { app.evidenceDirectory.listFiles()?.any { it.length()>0 } == true }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        screen.recreate(); ready(R.id.entry_title)
        onView(withId(R.id.capture)).perform(scrollTo()).check(matches(withText(R.string.retake)))
        Intents.intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(ActivityResult(Activity.RESULT_CANCELED,null))
        onView(withId(R.id.capture)).perform(click())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onView(withId(R.id.action_save)).perform(click()); ready(R.id.new_entry)
        val record=runBlocking { dao.watch().first().single() }
        assertNotNull(record.image); assertTrue(app.evidenceDirectory.resolve(record.image!!).exists())
        assertEquals(1,app.evidenceDirectory.listFiles()!!.size)
        onView(withText(record.heading)).perform(click()); ready(R.id.entry_title)
        Intents.intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(ActivityResult(Activity.RESULT_OK,null))
        onView(withId(R.id.capture)).perform(scrollTo(),click())
        await { app.evidenceDirectory.listFiles()!!.size == 2 }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        pressBack(); onView(withText(R.string.discard)).perform(click()); ready(R.id.new_entry)
        assertEquals(record.image,runBlocking { dao.read(record.key)!!.image })
        assertEquals(1,app.evidenceDirectory.listFiles()!!.size)
        onView(withText(record.heading)).perform(click()); ready(R.id.entry_title)
        openDelete(); onView(withText(R.string.delete)).perform(click()); ready(R.id.new_entry)
        assertTrue(app.evidenceDirectory.listFiles()!!.isEmpty())
        IntentMonitorRegistry.getInstance().removeIntentCallback(cameraWriter)
    }
    @Test fun unreadableCameraResultDoesNotReplaceSavedPhoto() {
        onView(withId(R.id.new_entry)).perform(click())
        ready(R.id.entry_title)
        onView(withId(R.id.entry_title)).perform(replaceText("Window latch"), closeSoftKeyboard())
        val invalidCamera = IntentCallback { intent ->
            if (intent.action == MediaStore.ACTION_IMAGE_CAPTURE) {
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
                app.contentResolver.openOutputStream(uri)!!.use { it.write("not an image".toByteArray()) }
            }
        }
        IntentMonitorRegistry.getInstance().addIntentCallback(invalidCamera)
        Intents.intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE))
            .respondWith(ActivityResult(Activity.RESULT_OK, null))
        try {
            onView(withId(R.id.capture)).perform(scrollTo(), click())
            await { app.evidenceDirectory.listFiles()!!.isEmpty() }
            ready(R.id.entry_title)
            onView(withId(R.id.capture)).check(matches(withText(R.string.camera)))
            onView(withId(R.id.action_save)).perform(click())
            ready(R.id.new_entry)
            assertNull(runBlocking { dao.watch().first().single().image })
        } finally {
            IntentMonitorRegistry.getInstance().removeIntentCallback(invalidCamera)
        }
    }

    @Test fun notesSearchFindsAnEntryWithoutMatchingItsTitle() {
        runBlocking {
            dao.write(Incident(heading = "Supply cupboard", notes = "Order more envelopes"))
            dao.write(Incident(heading = "Desk phone", notes = "Replace cable"))
        }
        onView(withId(R.id.search)).perform(replaceText("  envelopes  "), closeSoftKeyboard())
        awaitRows(1)
        onView(withId(R.id.row_title)).check(matches(withText("Supply cupboard")))
    }

    private fun openDelete() {
        openActionBarOverflowOrOptionsMenu(app)
        onView(withText(R.string.delete)).perform(click())
    }
    private fun ready(id: Int) = await {
        var ok=false
        screen.onActivity { val view=it.findViewById<View>(id);ok=view!=null && view.isShown && view.isEnabled }
        ok
    }
    private fun awaitRows(count: Int)=await {
        var ok=false
        screen.onActivity { ok=it.findViewById<RecyclerView>(R.id.entries)?.adapter?.itemCount==count }
        ok
    }
    private fun await(condition:()->Boolean) {
        val end=System.currentTimeMillis()+10000
        while(System.currentTimeMillis()<end) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if(condition()) return
            Thread.sleep(50)
        }
        fail("App did not reach the expected state")
    }
}
