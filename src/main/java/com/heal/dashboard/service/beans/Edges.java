package com.heal.dashboard.service.beans;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
public class Edges {
    String source;
    String target;
    Map<String, String> data = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edges)) return false;
        Edges edges = (Edges) o;
        return Objects.equals(this.source, edges.source) &&
                Objects.equals(this.target, edges.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.source, this.target);
    }

    public static Edges clone(Edges e) {
        Edges edges = new Edges();
        edges.setSource(e.getSource());
        edges.setTarget(e.getTarget());
        edges.setData(e.getData());

        return edges;
    }
}

