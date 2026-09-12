package com.example.cgpacalculator

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var yearSpinner: AutoCompleteTextView
    private lateinit var semesterSpinner: AutoCompleteTextView
    private lateinit var subjectCountSpinner: AutoCompleteTextView
    private lateinit var subjectContainer: LinearLayout

    private lateinit var btnCalculateSGPA: Button
    private lateinit var btnSaveSemester: Button
    private lateinit var btnCalculateCGPA: Button
    private lateinit var btnClear: Button

    private lateinit var txtSGPA: TextView
    private lateinit var txtCGPA: TextView
    private lateinit var txtSemesterResults: TextView

    private lateinit var studentPhoto: ImageView
    private lateinit var txtPhotoResult: TextView
    private lateinit var cardPhotoResult: View

    private lateinit var preferences: SharedPreferences

    private var calculationType = ""

    private val cameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                cameraLauncher.launch(null)
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->

            if (bitmap != null) {

                studentPhoto.setImageBitmap(bitmap)
                cardPhotoResult.visibility = View.VISIBLE

                if (calculationType == "SGPA") {

                    val sgpa = calculateSGPA()

                    if (sgpa > 0) {
                        txtPhotoResult.text =
                            "Student Result\n\nSGPA: %.2f".format(sgpa)
                    }

                } else if (calculationType == "CGPA") {

                    val cgpa = calculateCGPA()

                    if (cgpa > 0) {
                        txtPhotoResult.text =
                            "Student Result\n\nCGPA: %.2f".format(cgpa)
                    }
                }

                calculationType = ""
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        preferences =
            getSharedPreferences("CGPA_DATA", MODE_PRIVATE)

        initializeViews()
        setupYearDropdown()
        setupSemesterDropdown()
        setupSubjectCountDropdown()
        setupButtons()

        // Default: 3 subjects
        createSubjectRows(3)

        updateSemesterResults()
    }

    private fun initializeViews() {

        yearSpinner = findViewById(R.id.yearSpinner)
        semesterSpinner = findViewById(R.id.semesterSpinner)
        subjectCountSpinner = findViewById(R.id.subjectCountSpinner)

        subjectContainer = findViewById(R.id.subjectContainer)

        btnCalculateSGPA = findViewById(R.id.btnCalculateSGPA)
        btnSaveSemester = findViewById(R.id.btnSaveSemester)
        btnCalculateCGPA = findViewById(R.id.btnCalculateCGPA)
        btnClear = findViewById(R.id.btnClear)

        txtSGPA = findViewById(R.id.txtSGPA)
        txtCGPA = findViewById(R.id.txtCGPA)
        txtSemesterResults = findViewById(R.id.txtSemesterResults)

        studentPhoto = findViewById(R.id.studentPhoto)
        txtPhotoResult = findViewById(R.id.txtPhotoResult)
        cardPhotoResult = findViewById(R.id.cardPhotoResult)
    }

    private fun setupYearDropdown() {

        val years = arrayOf(
            "Year 1",
            "Year 2",
            "Year 3",
            "Year 4"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            years
        )

        yearSpinner.setAdapter(adapter)

        yearSpinner.setOnItemClickListener { _, _, position, _ ->
            updateSemestersForYear(years[position])
        }
    }

    private fun updateSemestersForYear(year: String) {
        val semesters = when (year) {
            "Year 1" -> arrayOf("Semester 1", "Semester 2")
            "Year 2" -> arrayOf("Semester 3", "Semester 4")
            "Year 3" -> arrayOf("Semester 5", "Semester 6")
            "Year 4" -> arrayOf("Semester 7", "Semester 8")
            else -> emptyArray()
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            semesters
        )

        semesterSpinner.setAdapter(adapter)
        semesterSpinner.setText("", false)
    }

    private fun setupSemesterDropdown() {
        // Initially empty until a year is selected
        semesterSpinner.setAdapter(null)
    }

    private fun setupSubjectCountDropdown() {

        val subjectCounts = arrayOf(
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "10"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            subjectCounts
        )

        subjectCountSpinner.setAdapter(adapter)

        subjectCountSpinner.setOnItemClickListener { _, _, position, _ ->
            val count = subjectCounts[position].toInt()
            createSubjectRows(count)
        }
    }

    private fun setupButtons() {

        btnCalculateSGPA.setOnClickListener {

            if (!validateSelection()) {
                return@setOnClickListener
            }

            if (!hasValidSubjects()) {

                Toast.makeText(
                    this,
                    "Please enter all subject details.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            calculationType = "SGPA"
            openCamera()
        }

        btnSaveSemester.setOnClickListener {

            if (!validateSelection()) {
                return@setOnClickListener
            }

            val sgpa = calculateSGPA()

            if (sgpa <= 0) {
                return@setOnClickListener
            }

            saveSemester()
        }

        btnCalculateCGPA.setOnClickListener {

            if (!hasSavedSemester()) {

                Toast.makeText(
                    this,
                    "Save at least one semester first.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            calculationType = "CGPA"
            openCamera()
        }

        btnClear.setOnClickListener {

            subjectContainer.removeAllViews()

            createSubjectRows(3)

            yearSpinner.setText("", false)
            semesterSpinner.setText("", false)
            subjectCountSpinner.setText("", false)

            txtSGPA.text = "SGPA: 0.00"
            txtCGPA.text = "Overall CGPA: 0.00"

            cardPhotoResult.visibility = View.GONE

            txtPhotoResult.text = ""
        }
    }

    private fun validateSelection(): Boolean {

        if (yearSpinner.text.toString().isEmpty()) {

            Toast.makeText(
                this,
                "Please select Year.",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        if (semesterSpinner.text.toString().isEmpty()) {

            Toast.makeText(
                this,
                "Please select Semester.",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        return true
    }

    private fun createSubjectRows(count: Int) {

        subjectContainer.removeAllViews()

        val inflater = layoutInflater

        for (i in 1..count) {

            val row = inflater.inflate(R.layout.item_subject, subjectContainer, false)

            val title = row.findViewById<TextView>(R.id.txtSubjectTitle)
            title.text = "Subject $i"

            subjectContainer.addView(row)
        }
    }

    private fun hasValidSubjects(): Boolean {

        if (subjectContainer.childCount == 0) {
            return false
        }

        for (i in 0 until subjectContainer.childCount) {

            val row = subjectContainer.getChildAt(i)

            val name = row.findViewById<EditText>(R.id.edtSubjectName)
            val credits = row.findViewById<EditText>(R.id.edtCredits)
            val marks = row.findViewById<EditText>(R.id.edtMarks)

            val subjectName = name.text.toString().trim()
            val creditValue = credits.text.toString().toDoubleOrNull()
            val marksValue = marks.text.toString().toDoubleOrNull()

            if (
                subjectName.isEmpty() ||
                creditValue == null ||
                creditValue <= 0 ||
                marksValue == null ||
                marksValue < 0 ||
                marksValue > 100
            ) {
                return false
            }
        }

        return true
    }

    private fun marksToPoint(marks: Double): Double {

        return when {

            marks >= 90 -> 10.0
            marks >= 80 -> 9.0
            marks >= 70 -> 8.0
            marks >= 60 -> 7.0
            marks >= 50 -> 6.0
            marks >= 40 -> 5.0
            else -> 0.0
        }
    }

    private fun calculateSGPA(): Double {

        var totalCredits = 0.0
        var totalPoints = 0.0

        for (i in 0 until subjectContainer.childCount) {

            val row = subjectContainer.getChildAt(i)

            val credits = row.findViewById<EditText>(R.id.edtCredits)
            val marks = row.findViewById<EditText>(R.id.edtMarks)

            val credit = credits.text.toString().toDoubleOrNull() ?: continue
            val mark = marks.text.toString().toDoubleOrNull() ?: continue

            val point = marksToPoint(mark)

            totalCredits += credit
            totalPoints += credit * point
        }

        if (totalCredits <= 0) {
            return 0.0
        }

        val sgpa = totalPoints / totalCredits

        txtSGPA.text = "SGPA: %.2f".format(sgpa)

        return sgpa
    }

    private fun totalCredits(): Double {

        var total = 0.0

        for (i in 0 until subjectContainer.childCount) {

            val row = subjectContainer.getChildAt(i)
            val credits = row.findViewById<EditText>(R.id.edtCredits)

            val value = credits.text.toString().toDoubleOrNull()

            if (value != null && value > 0) {
                total += value
            }
        }

        return total
    }

    private fun saveSemester() {

        val semesterText = semesterSpinner.text.toString()
        val semester = if (semesterText.startsWith("Semester ")) {
            semesterText.removePrefix("Semester ").toIntOrNull() ?: 0
        } else 0

        if (semester == 0) {
            Toast.makeText(this, "Invalid Semester selected.", Toast.LENGTH_SHORT).show()
            return
        }

        val sgpa = calculateSGPA()

        val credits =
            totalCredits()

        preferences.edit()
            .putFloat(
                "sgpa_$semester",
                sgpa.toFloat()
            )
            .putFloat(
                "credits_$semester",
                credits.toFloat()
            )
            .apply()

        Toast.makeText(
            this,
            "Semester saved successfully.",
            Toast.LENGTH_SHORT
        ).show()

        updateSemesterResults()
    }

    private fun calculateCGPA(): Double {

        var totalWeighted = 0.0
        var totalCredits = 0.0

        for (semester in 1..8) {

            val sgpa =
                preferences.getFloat(
                    "sgpa_$semester",
                    0f
                ).toDouble()

            val credits =
                preferences.getFloat(
                    "credits_$semester",
                    0f
                ).toDouble()

            if (
                sgpa > 0 &&
                credits > 0
            ) {

                totalWeighted +=
                    sgpa * credits

                totalCredits +=
                    credits
            }
        }

        if (totalCredits <= 0) {
            return 0.0
        }

        val cgpa =
            totalWeighted / totalCredits

        txtCGPA.text =
            "CGPA: %.2f".format(cgpa)

        return cgpa
    }

    private fun hasSavedSemester(): Boolean {

        for (semester in 1..8) {

            val sgpa =
                preferences.getFloat(
                    "sgpa_$semester",
                    0f
                )

            if (sgpa > 0) {
                return true
            }
        }

        return false
    }

    private fun updateSemesterResults() {

        val result =
            StringBuilder()

        result.append(
            "Saved Semester Results\n\n"
        )

        var count = 0

        for (semester in 1..8) {

            val sgpa =
                preferences.getFloat(
                    "sgpa_$semester",
                    0f
                )

            val credits =
                preferences.getFloat(
                    "credits_$semester",
                    0f
                )

            if (sgpa > 0) {

                result.append(
                    "Semester $semester  -  " +
                            "SGPA: %.2f  " +
                            "Credits: %.1f\n"
                                .format(
                                    sgpa,
                                    credits
                                )
                )

                count++
            }
        }

        if (count == 0) {

            result.append(
                "No semester saved yet."
            )
        }

        txtSemesterResults.text =
            result.toString()
    }

    private fun openCamera() {

        if (
            checkSelfPermission(
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            cameraLauncher.launch(null)

        } else {

            cameraPermission.launch(
                Manifest.permission.CAMERA
            )
        }
    }
}