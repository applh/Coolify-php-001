<?php

abstract class Model {
    // The database table associated with the model.
    // Should be defined in child classes.
    protected static $table;

    /**
     * Get all records from the table
     */
    public static function all() {
        $sql = "SELECT * FROM " . static::$table;
        return DB::query($sql)->fetchAll();
    }

    /**
     * Find a record by its primary key (ID)
     */
    public static function find($id) {
        $sql = "SELECT * FROM " . static::$table . " WHERE id = ?";
        return DB::query($sql, [$id])->fetch();
    }

    /**
     * Delete a record by its primary key (ID)
     */
    public static function delete($id) {
        $sql = "DELETE FROM " . static::$table . " WHERE id = ?";
        return DB::query($sql, [$id])->rowCount();
    }
    
    // Add additional CRUD methods (insert, update) as needed based on the model's attributes.
}
