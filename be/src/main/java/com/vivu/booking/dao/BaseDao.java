package com.vivu.booking.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import com.vivu.booking.config.HibernateConfig;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class BaseDao<T, ID> {

    protected final Class<T> entityClass;

    protected BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected SessionFactory sf() {
        return HibernateConfig.getSessionFactory();
    }

    protected <R> R tx(Function<Session, R> work) {
        try (Session s = sf().openSession()) {
            var tx = s.beginTransaction();
            try {
                R r = work.apply(s);
                tx.commit();
                return r;
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    protected <R> R read(Function<Session, R> work) {
        try (Session s = sf().openSession()) {
            return work.apply(s);
        }
    }

    public Optional<T> findById(ID id) {
        return read(s -> Optional.ofNullable(s.find(entityClass, id)));
    }

    public List<T> findAll(int page, int size) {
        return read(s -> s.createQuery("from " + entityClass.getSimpleName() + " order by id asc", entityClass)
                .setFirstResult(page * size).setMaxResults(size).getResultList());
    }

    public long countAll() {
        return read(s -> s.createQuery("select count(e) from " + entityClass.getSimpleName() + " e", Long.class)
                .getSingleResult());
    }

    public T save(T entity) {
        return tx(s -> {
            s.persist(entity);
            return entity;
        });
    }

    public T update(T entity) {
        return tx(s -> s.merge(entity));
    }

    public void deleteById(ID id) {
        tx(s -> {
            T e = s.find(entityClass, id);
            if (e != null) s.remove(e);
            return null;
        });
    }
}
