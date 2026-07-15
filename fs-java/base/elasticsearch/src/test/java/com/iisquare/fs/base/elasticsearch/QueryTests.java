package com.iisquare.fs.base.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.Test;

public class QueryTests {

    @Test
    public void termTest() {
        Query q1 = Query.of(q -> { // b会覆盖a
            q.term(t -> t.field("a").value(1));
            q.term(t -> t.field("b").value(2));
            return q;
        });
        System.out.println(q1.toString()); // Query: {"term":{"b":{"value":1}}}
        Query q2 = Query.of(q -> q.bool(b -> b.must(m -> { // b会覆盖a
            m.term(t -> t.field("a").value(1));
            m.term(t -> t.field("b").value(2));
            return m;
        })));
        System.out.println(q2.toString()); // Query: {"bool":{"must":[{"term":{"b":{"value":1}}}]}}
        Query q3 = Query.of(q -> q.bool(b -> { // 同时保留a和b
            b.must(m -> m.term(t -> t.field("a").value(1)));
            b.must(m -> m.term(t -> t.field("b").value(2)));
            return b;
        }));
        System.out.println(q3.toString()); // Query: {"bool":{"must":[{"term":{"a":{"value":1}}},{"term":{"b":{"value":2}}}]}}
    }

}
