package io.objectbox.paginate;

import android.arch.paging.DataSource;
import io.objectbox.Box;
import io.objectbox.query.Query;

/**
 * Default implementation of positional DataSource.Factory that creates #LimitOffsetDataSource from Box or Query instance
 *
 * @param <T> Indicates type of data
 * @author Ekalips
 */
public class DefaultPositionalDataSourceFactory<T> implements DataSource.Factory<Integer, T> {

    private final Query<T> query;

    private DefaultPositionalDataSourceFactory(Box<T> box) {
        this(box.query().build());
    }

    private DefaultPositionalDataSourceFactory(Query<T> query) {
        this.query = query;
    }

    @Override
    public DataSource<Integer, T> create() {
        return new LimitOffsetDataSource<>(query);
    }


    /**
     * Uses box as data source for {@link #DefaultPositionalDataSourceFactory(Box)}. Box is used to extract base query (box.query().build())
     *
     * @see #DefaultPositionalDataSourceFactory(Query)
     */
    public static <T> DefaultPositionalDataSourceFactory<T> create(Box<T> box) {
        return new DefaultPositionalDataSourceFactory<>(box);
    }

    /**
     * Creates instance of {@link #DefaultPositionalDataSourceFactory(Query)} from provided #Query
     *
     * @param query Data source that will be used in #LimitOffsetDataSource
     * @param <T>   Returned data type
     * @return Returns instance of {@link #DefaultPositionalDataSourceFactory(Query)} with query as data source
     */
    public static <T> DefaultPositionalDataSourceFactory<T> craete(Query<T> query) {
        return new DefaultPositionalDataSourceFactory<>(query);
    }
}
