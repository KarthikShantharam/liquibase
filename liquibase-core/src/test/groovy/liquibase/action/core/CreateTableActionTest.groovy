package liquibase.action.core

import liquibase.JUnitScope
import liquibase.actionlogic.ActionExecutor
import liquibase.database.ConnectionSupplierFactory
import liquibase.structure.ObjectName
import liquibase.structure.core.Table
import liquibase.util.CollectionUtil
import spock.lang.Specification
import spock.lang.Unroll
import testmd.TestMD
import testmd.logic.SetupResult

class CreateTableActionTest extends Specification {

    def "empty constructor"() {
        expect:
        new CreateTableAction().describe() == "createTable()"
    }

    def "parametrized constructor"() {
        expect:
        new CreateTableAction(new ObjectName("cat", "schem", "tab")).describe() == "createTable(tableName=cat.schem.tab)"
    }

    @Unroll("#featureName #tableName")
    def "create simple table"() {
        expect:

        def action = new CreateTableAction(tableName)
                .addColumn(new ColumnDefinition("id", "int"))

        def scope = JUnitScope.getInstance(conn.getDatabase())
        def plan = new ActionExecutor().createPlan(action, scope)

        TestMD.test(this.class, "create simple table ${conn.databaseShortName}", conn.getDatabase().class)
                .permutation([connection: conn, tableName: tableName.toString()])
                .asTable("tableName")
                .addResult("plan", plan)
                .forceRun()
                .setup({
            conn.connect(scope)
            throw SetupResult.OK
        })
                .cleanup({
            new ActionExecutor().execute(new DropTableAction(tableName), scope)
        })
                .run({
            plan.execute(scope)

        })


        where:
        [conn, tableName] << JUnitScope.instance.getSingleton(ConnectionSupplierFactory).connectionSuppliers.collectMany {
            return CollectionUtil.permutations([
                    [it],
                    it.getObjectNames(Table.class, true)
            ])
        }
    }
}
