# Sports Partner Finder Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the app from mock data to a fully connected database-driven system with bottom navigation and real comments.

**Architecture:** Refactor `MainActivity` into a `BottomNavigationView` container hosting four Fragments (`Map`, `Create`, `MyPosts`, `Profile`). Replace `GamePost` mock with real `Event` and `Comment` entities using Room.

**Tech Stack:** Kotlin, Android SDK, Room (SQLite), Google Maps SDK, Material3.

---

### Task 1: New Data Layer - Comment Entity and DAO

**Files:**
- Create: `app/src/main/java/com/example/playground/data/Comment.kt`
- Create: `app/src/main/java/com/example/playground/data/CommentDao.kt`
- Modify: `app/src/main/java/com/example/playground/data/AppDatabase.kt`

- [ ] **Step 1: Create Comment Entity**

```kotlin
package com.example.playground.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("authorId")]
)
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val authorId: Long,
    val content: String,
    val timestamp: Long
)
```

- [ ] **Step 2: Create CommentDao**

```kotlin
package com.example.playground.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CommentDao {
    @Insert
    fun insertComment(comment: Comment): Long

    @Query("SELECT * FROM comments WHERE eventId = :eventId ORDER BY timestamp DESC")
    fun getCommentsByEvent(eventId: Long): List<Comment>
}
```

- [ ] **Step 3: Update AppDatabase**

Register the new entity and DAO.

```kotlin
// In AppDatabase.kt
@Database(entities = [User::class, Event::class, Comment::class], version = 1) // Added Comment::class
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun commentDao(): CommentDao // Added this line
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/playground/data/Comment.kt \
        app/src/main/java/com/example/playground/data/CommentDao.kt \
        app/src/main/java/com/example/playground/data/AppDatabase.kt
git commit -m "feat: add Comment entity and DAO"
```

### Task 2: Enhance Event Entity and Repository

**Files:**
- Modify: `app/src/main/java/com/example/playground/data/Event.kt`
- Modify: `app/src/main/java/com/example/playground/repository/EventRepository.kt`

- [ ] **Step 1: Update Event Entity**

Add `address` and `published` fields.

```kotlin
// In Event.kt
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hostId: Long,
    val sport: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val maxPlayers: Int,
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String,
    val address: String? = null,
    val published: Boolean = true
)
```

- [ ] **Step 2: Update EventRepository**

Add methods for comments and fetch logic.

```kotlin
// In EventRepository.kt
fun postComment(eventId: Long, authorId: Long, content: String): EventResult {
    val comment = Comment(
        eventId = eventId,
        authorId = authorId,
        content = content,
        timestamp = System.currentTimeMillis()
    )
    val id = commentDao.insertComment(comment)
    return EventResult.Success(id)
}

fun getCommentsForEvent(eventId: Long): List<Comment> = commentDao.getCommentsByEvent(eventId)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/playground/data/Event.kt \
        app/src/main/java/com/example/playground/repository/EventRepository.kt
git commit -m "feat: enhance Event entity and update EventRepository"
```

### Task 3: Bottom Navigation and Fragment Structure

**Files:**
- Create: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/example/playground/MainActivity.kt`

- [ ] **Step 1: Create Navigation Menu**

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/nav_map"
        android:icon="@android:drawable/ic_dialog_map"
        android:title="Map" />
    <item
        android:id="@+id/nav_create"
        android:icon="@android:drawable/ic_input_add"
        android:title="Create" />
    <item
        android:id="@+id/nav_my_posts"
        android:icon="@android:drawable/ic_menu_edit"
        android:title="My Posts" />
    <item
        android:id="@+id/nav_profile"
        android:icon="@android:drawable/ic_menu_myplaces"
        android:title="Profile" />
</menu>
```

- [ ] **Step 2: Update activity_main.xml**

Replace old layout with `BottomNavigationView` and a `FragmentContainerView`.

- [ ] **Step 3: Update MainActivity.kt**

Refactor to manage fragment transactions.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/menu/bottom_nav_menu.xml \
        app/src/main/res/layout/activity_main.xml \
        app/src/main/java/com/example/playground/MainActivity.kt
git commit -m "feat: implement Bottom Navigation and Fragment structure in MainActivity"
```

### Task 4: Implement MapFragment (Real Data & Filtering)

**Files:**
- Create: `app/src/main/java/com/example/playground/ui/map/MapFragment.kt`
- Create: `app/src/main/res/layout/fragment_map.xml`

- [ ] **Step 1: Implement fragment_map.xml**

Mirror the layout from `activity_main.xml` (the map, filter chips, search bar, and details card).

- [ ] **Step 2: Implement MapFragment.kt**

Port the map logic from the old `MainActivity.kt`, but replace `gamePosts` list with `eventRepository.getAllEvents()`. Update marker tagging to use the `Event` entity.

- [ ] **Step 3: Update Marker Rendering**

Ensure markers show `0/${event.maxPlayers}` and use sport icons (Basketball icon for "Basketball", etc.).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/playground/ui/map/MapFragment.kt \
        app/src/main/res/layout/fragment_map.xml
git commit -m "feat: implement MapFragment with real database data"
```

### Task 5: Implement Create, MyPosts, and Profile Fragments

**Files:**
- Create: `app/src/main/java/com/example/playground/ui/home/CreateEventFragment.kt`
- Create: `app/src/main/java/com/example/playground/ui/myposts/MyPostsFragment.kt`
- Create: `app/src/main/java/com/example/playground/ui/profile/ProfileFragment.kt`

- [ ] **Step 1: CreateEventFragment**

Port the creation logic from `CreateEventActivity`. Update fields to match Figma (Description, Address).

- [ ] **Step 2: MyPostsFragment**

Fetch events from `eventRepository.getAllEvents()` and filter by `hostId == currentUser.id`. Display in a `RecyclerView`.

- [ ] **Step 3: ProfileFragment**

Display user info and Logout button.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/playground/ui/home/CreateEventFragment.kt \
        app/src/main/java/com/example/playground/ui/myposts/MyPostsFragment.kt \
        app/src/main/java/com/example/playground/ui/profile/ProfileFragment.kt
git commit -m "feat: implement Create, MyPosts, and Profile fragments"
```

### Task 6: Final Verification and Cleanup

- [ ] **Step 1: Final Cleanup**

Remove obsolete files: `MockGamePosts.kt`, `GamePost.kt`, `GamePostsAdapter.kt`.

- [ ] **Step 2: End-to-End Verification**

Verify creation, map markers, and my posts list.

- [ ] **Step 3: Commit**

```bash
git commit -m "chore: cleanup mock files and finalize redesign refactor"
```
