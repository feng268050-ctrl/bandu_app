package com.bandu.tiji.core.storage.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.bandu.tiji.core.storage.db.dao.CollectionDao
import com.bandu.tiji.core.storage.db.dao.ErrorItemDao
import com.bandu.tiji.core.storage.db.dao.ErrorItemPagingDao
import com.bandu.tiji.core.storage.db.dao.QuestionBankDao
import com.bandu.tiji.core.storage.db.dao.TagDao
import com.bandu.tiji.core.storage.db.dao.StatsDao
import com.bandu.tiji.core.storage.db.dao.TutorDao
import com.bandu.tiji.core.storage.db.entity.BankQuestionEntity
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemTagEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemFtsEntity
import com.bandu.tiji.core.storage.db.entity.ExamAttemptEntity
import com.bandu.tiji.core.storage.db.entity.ExamSessionEntity
import com.bandu.tiji.core.storage.db.entity.ExerciseEntity
import com.bandu.tiji.core.storage.db.entity.QuestionBankEntity
import com.bandu.tiji.core.storage.db.entity.TagEntity
import com.bandu.tiji.core.storage.db.entity.TutorMessageEntity
import com.bandu.tiji.core.storage.db.entity.TutorSessionEntity

@Database(
    entities = [
        CollectionEntity::class,
        ErrorItemEntity::class,
        ErrorItemFtsEntity::class,
        TagEntity::class,
        ErrorItemTagEntity::class,
        TutorSessionEntity::class,
        TutorMessageEntity::class,
        ExerciseEntity::class,
        QuestionBankEntity::class,
        BankQuestionEntity::class,
        ExamSessionEntity::class,
        ExamAttemptEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao

    abstract fun errorItemDao(): ErrorItemDao

    abstract fun errorItemPagingDao(): ErrorItemPagingDao

    abstract fun tagDao(): TagDao

    abstract fun tutorDao(): TutorDao

    abstract fun statsDao(): StatsDao

    abstract fun questionBankDao(): QuestionBankDao
}
