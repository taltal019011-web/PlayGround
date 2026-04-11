# Design: Sports Partner Finder Redesign

Moving from mock-based `MainActivity` to a fully connected Sports Partner Finder app with bottom navigation.

## Overview

The current application uses a mock-based system (`GamePost`) for displaying map markers and details. This design document outlines the steps to refactor the application to use the real `Event` entity from the database, implement a real `Comment` system, and redesign the UI based on the Figma-provided screenshots.

## Architecture

The `MainActivity` will be refactored to host a `BottomNavigationView` with four fragments:

1.  **`MapFragment`**: The primary map view with advanced filtering (Sport Type, Time Range).
2.  **`CreateEventFragment`**: Moving the creation logic from an Activity to a Fragment for a more seamless experience.
3.  **`MyPostsFragment`**: A new screen for users to manage their own created events.
4.  **`ProfileFragment`**: User information and logout functionality.

## Data Model Refactor

### New Entity: `Comment`

To replace the mock comment system, we will introduce a new `Comment` table.

```kotlin
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

### Event Entity Enhancement

The `Event` entity will be updated to include more descriptive fields matching the new UI.

```kotlin
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
    val address: String?, // Added field for "Court Address/Name"
    val published: Boolean = true
)
```

## UI Refactor

### Map (MapFragment)

The map screen will be updated with:

*   **Filter Bars**: Two horizontal scrollable bars for "Sport Type" and "Time Range".
*   **Custom Map Markers**: Icon-based markers (e.g., Basketball icon) with the `maxPlayers` count.
*   **Real Data Binding**: Markers will be loaded directly from the `Event` database.
*   **Details Card Binding**: Pulling real data for the host name, event title, description, and the list of real comments.

### Create (CreateEventFragment)

Creation logic will be moved to a fragment, with fields matching the Figma design:
*   Sport Selection (including "Other").
*   Description (large text area).
*   Address/Court Name.
*   Location picker (using current location or custom).

### My Posts (MyPostsFragment)

A simple list of the user's events, providing a way to edit or delete their own posts.

### Profile (ProfileFragment)

A dedicated space for the user's profile information and the logout button.

## Implementation Steps

### Phase 1: Database and Domain

1.  Create `Comment.kt` and `CommentDao.kt`.
2.  Update `Event.kt` with the `address` field.
3.  Register new entities in `AppDatabase.kt`.
4.  Update `EventRepository.kt` with `getEventWithComments()` and `postComment()`.

### Phase 2: Navigation Refactor

1.  Create the `menu` resource for `BottomNavigationView`.
2.  Refactor `activity_main.xml` to include a `FragmentContainerView` and `BottomNavigationView`.
3.  Refactor `MainActivity.kt` to handle fragment switching.

### Phase 3: Fragment Implementations

1.  Implement `MapFragment`: Move map logic from `MainActivity`, add filter bars, and update marker rendering.
2.  Implement `CreateEventFragment`: Port logic from `CreateEventActivity`.
3.  Implement `MyPostsFragment`: Create a simple adapter for listing user-owned events.
4.  Implement `ProfileFragment`: Add basic user information and the logout button.

### Phase 4: Connected Features

1.  Implement real database-driven filtering logic in `MapFragment`.
2.  Connect the creation UI to the `EventRepository`.
3.  Add the ability to "Join" an event (if needed later, but focusing on `maxPlayers` for now).

## Verification Plan

1.  **Data Persistence**: Create an event and verify it exists in the database.
2.  **Navigation**: Ensure all bottom nav tabs switch correctly.
3.  **Real-Time Map Update**: After creating an event, the map in the Map tab should automatically show the new marker.
4.  **Correct Details**: Verify the details card shows the correct host name and comments for the selected event.
