package com.techmath.ticktask
import android.content.Context
import java.io.*

class FileHelper {
    private val FILENAME = "tasks.dat"

    fun writeData(taskList: ArrayList<Task>, context: Context) {
        try {
            val fos: FileOutputStream = context.openFileOutput(FILENAME, Context.MODE_PRIVATE)
            val oos = ObjectOutputStream(fos)
            oos.writeObject(taskList)
            oos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readData(context: Context): ArrayList<Task> {
        var taskList: ArrayList<Task> = ArrayList()

        try {
            val fis: FileInputStream = context.openFileInput(FILENAME)
            val ois = ObjectInputStream(fis)
            taskList = ois.readObject() as ArrayList<Task>
            ois.close()
        } catch (e: FileNotFoundException) {
            // File doesn't exist yet, return an empty list
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }

        return taskList
    }
}