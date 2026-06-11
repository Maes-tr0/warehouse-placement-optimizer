package com.nikitaopara.warehouseoptimizer.warehouse.routing.service;

import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRoute;
import com.nikitaopara.warehouseoptimizer.warehouse.routing.model.WarehouseRouteNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DijkstraWarehouseRouter {

    Map<String, Integer> calculateDistances(WarehouseGraph graph, String sourceNodeId) {
        if (graph.getNode(sourceNodeId) == null) {
            throw new IllegalArgumentException("Route source node does not exist: " + sourceNodeId);
        }

        Map<String, Integer> distances = initializeDistances(graph, sourceNodeId);
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
                Comparator.comparingInt(NodeDistance::distanceMm)
        );
        queue.add(new NodeDistance(sourceNodeId, 0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();

            if (current.distanceMm() != distances.get(current.nodeId())) {
                continue;
            }

            for (WarehouseGraph.RouteEdge edge : graph.getEdges(current.nodeId())) {
                int candidateDistance = Math.addExact(current.distanceMm(), edge.distanceMm());

                if (candidateDistance < distances.get(edge.targetNodeId())) {
                    distances.put(edge.targetNodeId(), candidateDistance);
                    queue.add(new NodeDistance(edge.targetNodeId(), candidateDistance));
                }
            }
        }

        return distances;
    }

    Optional<WarehouseRoute> findShortestRoute(
            WarehouseGraph graph,
            String sourceNodeId,
            String targetNodeId
    ) {
        if (graph.getNode(sourceNodeId) == null || graph.getNode(targetNodeId) == null) {
            return Optional.empty();
        }

        Map<String, Integer> distances = initializeDistances(graph, sourceNodeId);
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
                Comparator.comparingInt(NodeDistance::distanceMm)
        );
        queue.add(new NodeDistance(sourceNodeId, 0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();

            if (current.distanceMm() != distances.get(current.nodeId())) {
                continue;
            }
            if (current.nodeId().equals(targetNodeId)) {
                break;
            }

            for (WarehouseGraph.RouteEdge edge : graph.getEdges(current.nodeId())) {
                int candidateDistance = Math.addExact(current.distanceMm(), edge.distanceMm());

                if (candidateDistance < distances.get(edge.targetNodeId())) {
                    distances.put(edge.targetNodeId(), candidateDistance);
                    previous.put(edge.targetNodeId(), current.nodeId());
                    queue.add(new NodeDistance(edge.targetNodeId(), candidateDistance));
                }
            }
        }

        Integer distance = distances.get(targetNodeId);
        if (distance == null || distance == Integer.MAX_VALUE) {
            return Optional.empty();
        }

        return Optional.of(new WarehouseRoute(
                reconstructRoute(graph, previous, sourceNodeId, targetNodeId),
                distance
        ));
    }

    private Map<String, Integer> initializeDistances(WarehouseGraph graph, String sourceNodeId) {
        Map<String, Integer> distances = new HashMap<>();
        graph.getNodes().forEach(node -> distances.put(node.id(), Integer.MAX_VALUE));
        distances.put(sourceNodeId, 0);
        return distances;
    }

    private List<WarehouseRouteNode> reconstructRoute(
            WarehouseGraph graph,
            Map<String, String> previous,
            String sourceNodeId,
            String targetNodeId
    ) {
        Deque<WarehouseRouteNode> route = new ArrayDeque<>();
        String currentNodeId = targetNodeId;

        while (currentNodeId != null) {
            route.addFirst(graph.getNode(currentNodeId));
            if (currentNodeId.equals(sourceNodeId)) {
                break;
            }
            currentNodeId = previous.get(currentNodeId);
        }

        return new ArrayList<>(route);
    }

    private record NodeDistance(String nodeId, int distanceMm) {
    }
}
