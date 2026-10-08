package dev.barboza.pulso.rastro;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Gancho do Hibernate: recebe cada SQL antes de ir ao banco. Registrado em application.properties. */
public class InspetorDeSql implements StatementInspector {

    @Override
    public String inspect(String sql) {
        RastroSql.registrar(sql);
        return sql;
    }
}
