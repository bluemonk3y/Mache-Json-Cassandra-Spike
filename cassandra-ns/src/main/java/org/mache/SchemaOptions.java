package org.mache;

/**
 * Created on 14/07/2015.
 */
public enum SchemaOptions {
    CREATESCHEMAIFNEEDED(true, false),
    USEEXISTINGSCHEMA(false, false),
    CREATEANDDROPSCHEMA(true, true);

    private boolean createSchema;
    private boolean dropSchema;

    SchemaOptions(boolean createSchema, boolean dropSchema) {
        this.createSchema = createSchema;

        this.dropSchema = dropSchema;
    }

    public boolean ShouldCreateSchema() {
        return createSchema;
    }
    public boolean ShouldDropSchema() {
        return dropSchema;
    }
}
