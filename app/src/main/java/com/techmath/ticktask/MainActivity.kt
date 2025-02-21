package com.techmath.ticktask

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var taskList: ArrayList<Task>
    private lateinit var listView: ListView
    private lateinit var addButton: Button
    private lateinit var editText: EditText
    private lateinit var dateTextView: TextView
    private lateinit var adapter: ArrayAdapter<String>
    private var selectedTimestamp: Long = 0
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission is required for reminders", Toast.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.sleep(2000L)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkNotificationPermission()
        setupViews()
        loadTasks()
        setupListeners()
        rescheduleAllReminders()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupViews() {
        listView = findViewById(R.id.listView)
        addButton = findViewById(R.id.addButton)
        editText = findViewById(R.id.editText)
        dateTextView = findViewById(R.id.dateTextView)
        dateTextView.text = "Select Date & Time"
    }

    private fun loadTasks() {
        taskList = FileHelper().readData(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            taskList.map { it.title + " - " + getFormattedDate(it.timestamp) })
        listView.adapter = adapter
    }

    private fun setupListeners() {
        dateTextView.setOnClickListener { pickDateTime() }

        addButton.setOnClickListener {
            val taskTitle = editText.text.toString().trim()
            if (taskTitle.isEmpty() || selectedTimestamp == 0L) {
                Toast.makeText(this, "Enter task and select reminder time!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val taskId = UUID.randomUUID().toString()
            val taskDescription = "Reminder for your task"
            val task = Task(taskId, taskTitle, taskDescription, selectedTimestamp)
            taskList.add(task)

            FileHelper().writeData(taskList, this)
            scheduleReminder(this, task)

            updateListView()
            editText.text.clear()
            dateTextView.text = "Select Date & Time"
            selectedTimestamp = 0
            Toast.makeText(this, "Task Added & Reminder Set!", Toast.LENGTH_SHORT).show()
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteDialog(position)
            true
        }
    }

    private fun pickDateTime() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute, 0)
                selectedTimestamp = calendar.timeInMillis
                dateTextView.text = getFormattedDate(selectedTimestamp)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun rescheduleAllReminders() {
        WorkManager.getInstance(applicationContext).cancelAllWork()

        taskList.forEach { task ->
            if (task.timestamp > System.currentTimeMillis()) {
                scheduleReminder(this, task)
            }
        }
    }

    private fun scheduleReminder(context: Context, task: Task) {
        val delay = task.timestamp - System.currentTimeMillis()
        if (delay > 0) {
            val data = workDataOf(
                "task_id" to task.id,
                "task_title" to task.title,
                "task_description" to task.description
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .setConstraints(constraints)
                .addTag(task.id)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    task.id,
                    ExistingWorkPolicy.REPLACE,
                    reminderRequest
                )
        }
    }

    private fun showDeleteDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Yes") { _, _ ->
                val removedTask = taskList[position]
                WorkManager.getInstance(applicationContext)
                    .cancelUniqueWork(removedTask.id)

                taskList.removeAt(position)
                FileHelper().writeData(taskList, this)
                updateListView()
                Toast.makeText(this, "Task Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateListView() {
        adapter.clear()
        adapter.addAll(taskList.map { it.title + " - " + getFormattedDate(it.timestamp) })
        adapter.notifyDataSetChanged()
    }

    private fun getFormattedDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

