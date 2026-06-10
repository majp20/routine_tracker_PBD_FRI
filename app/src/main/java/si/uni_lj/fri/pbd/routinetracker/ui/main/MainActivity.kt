package si.uni_lj.fri.pbd.routinetracker.ui.main


import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.navigation.NavigationView
import si.uni_lj.fri.pbd.routinetracker.R
import si.uni_lj.fri.pbd.routinetracker.databinding.ActivityMainBinding
import util.RoutineEvaluationWorker
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    //must be seen throughout the whole class
    private lateinit var binding: ActivityMainBinding // android automatically generates a class for every xml, allowing acces like binding.id
    lateinit var drawerLayout: DrawerLayout //lateinit variable will initialise later
    lateinit var toolbar: Toolbar

    private lateinit var appBarConfiguration: AppBarConfiguration

    lateinit var navigationView : NavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //creates notification channel
        createNotificationChannel()
        //request permission to send notifications
        requestNotificationPermission()
        //request 2nd
        requestExactAlarmPermissionIfNeeded()

        //navigation controller manages navigations between destinations
        //defined in nav_graph, using the navHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        //check for notifications
        notificationIntent(navController, intent)

        drawerLayout = binding.drawerLayout
        toolbar = binding.toolbar
        navigationView = binding.navigationView

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(toolbar)

        //screens reachable from the drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.routineListFragment, R.id.settingsFragment),
            drawerLayout
        )
        //connects the navigation view with the navController
        navigationView.setupWithNavController(navController)
        setupActionBarWithNavController(navController, appBarConfiguration)

        //scheduling workManager
        val workRequest =
            PeriodicWorkRequestBuilder<RoutineEvaluationWorker>(
                1, TimeUnit.HOURS
            ).build()
        //No constraints were listed
        //unique prevents multiple workers and duplicate scheduling
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RoutineEvaluationWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    //this function manages the "Up arrow", from android official docs
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    //here we will store last foreground time
    override fun onResume() {
        //mode private so only this app can read/write this data
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        super.onResume()
        editor.putLong("lastForegroundTime", System.currentTimeMillis()).apply()
    }

    //and last background time using shared preferences
    override fun onPause() {
        super.onPause()
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("lastBackgroundTime", System.currentTimeMillis()).apply()
    }



    //creates a channel that notifications can be sent to, function from android docs
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "routine_channel", "Routine Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ) //id, name, importance -> how intrusive notifications are

            //creates new notification channel
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    //requests permission from user to send notifications
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
    //requests exact alarm scheduling
    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {

                //creates an intent that can open settings screen
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent) //oppens the settings screen
            }
        }
    }

    //checks if the app was opened from a notification, and directs user to routineDetailsFragment for
    //specific routine
    private fun notificationIntent(navController: NavController, intent: Intent?) {
        val id = intent?.getLongExtra("routine_id", -1L) ?: -1L
        if(id == -1L) return
        val bundle = Bundle().apply {
                putLong("id", id)
        }
        navController.navigate(R.id.routineDetailsFragment, bundle)
        //removes possible hanging extras
        intent?.removeExtra("routine_id")
    }

    //enables notification opening even while the user is inside the app
    //defines what happens when a user is in the app and the notification triggers and it is clicked
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) //updated activity current intent

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        notificationIntent(navController, intent)
    }
}