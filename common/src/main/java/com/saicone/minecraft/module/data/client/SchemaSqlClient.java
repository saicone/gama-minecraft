/*
 * MIT License.
 *
 * Copyright (c) 2026 Rubenicos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.saicone.minecraft.module.data.client;

import com.saicone.minecraft.module.data.sql.SqlSchema;
import com.saicone.minecraft.module.data.sql.SqlType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public abstract class SchemaSqlClient extends SimpleSqlClient {

    private final SqlSchema schema;

    protected SchemaSqlClient() {
        this(SqlType.MYSQL);
    }

    protected SchemaSqlClient(@NotNull SqlType defaultType) {
        this(new SqlSchema(defaultType));
    }

    protected SchemaSqlClient(@NotNull SqlSchema schema) {
        this.schema = schema;
        this.defaultType = schema.getDefaultType();
    }

    @NotNull
    public SqlSchema schema() {
        return schema;
    }

    @NotNull
    protected String schema(@NotNull String key) {
        return parse(this.schema.get(this.type(), key));
    }

    @Override
    protected void onLoad() {
        super.onLoad();
        if (!this.schema.isLoaded()) {
            try {
                loadSchema();
            } catch (IOException e) {
                throw new RuntimeException("Cannot load SQL schema", e);
            }
        }
    }

    protected abstract void loadSchema() throws IOException;
}
