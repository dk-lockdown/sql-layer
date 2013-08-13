/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.sql.optimizer.rule;

import com.foundationdb.server.types3.Types3Switch;
import com.foundationdb.sql.NamedParamsTestBase;
import com.foundationdb.sql.TestBase;

import com.foundationdb.sql.optimizer.FunctionsTypeComputer;
import com.foundationdb.sql.optimizer.NestedResultSetTypeComputer;
import com.foundationdb.sql.optimizer.OptimizerTestBase;
import com.foundationdb.sql.optimizer.plan.AST;
import com.foundationdb.sql.optimizer.plan.PlanToString;
import com.foundationdb.sql.optimizer.rule.PlanContext;

import com.foundationdb.sql.parser.DMLStatementNode;
import com.foundationdb.sql.parser.StatementNode;

import com.foundationdb.ais.model.AkibanInformationSchema;
import com.foundationdb.server.service.functions.FunctionsRegistryImpl;

import com.foundationdb.junit.NamedParameterizedRunner;
import com.foundationdb.junit.NamedParameterizedRunner.TestParameters;
import com.foundationdb.junit.Parameterization;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;

@RunWith(NamedParameterizedRunner.class)
public class RulesTest extends OptimizerTestBase
                       implements TestBase.GenerateAndCheckResult
{
    public static final File RESOURCE_DIR = 
        new File(OptimizerTestBase.RESOURCE_DIR, "rule");

    protected File rulesFile, schemaFile, indexFile, statsFile, propertiesFile, extraDDL;

    @TestParameters
    public static Collection<Parameterization> statements() throws Exception {
        Collection<Object[]> result = new ArrayList<>();
        for (File subdir : RESOURCE_DIR.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            })) {
            File rulesFile;
            if (Types3Switch.ON) {
                rulesFile = new File (subdir, "t3rules.yml");
                if (!rulesFile.exists()) 
                    rulesFile = new File (subdir, "rules.yml");
            } else {
                rulesFile = new File(subdir, "rules.yml");
            }
            File schemaFile = new File(subdir, "schema.ddl");
            if (rulesFile.exists() && schemaFile.exists()) {
                File defaultStatsFile = new File(subdir, "stats.yaml");
                File defaultPropertiesFile = new File(subdir, "compiler.properties");
                File defaultExtraDDL = new File(subdir, "schema-extra.ddl");
                if (!defaultStatsFile.exists())
                    defaultStatsFile = null;
                if (!defaultPropertiesFile.exists())
                    defaultPropertiesFile = null;
                if (!defaultExtraDDL.exists())
                    defaultExtraDDL = null;
                for (Object[] args : sqlAndExpected(subdir)) {
                    File statsFile = new File(subdir, args[0] + ".stats.yaml");
                    File propertiesFile = new File(subdir, args[0] + ".properties");
                    File extraDDL = new File(subdir, args[0] + ".ddl");
                    if (!statsFile.exists())
                        statsFile = defaultStatsFile;
                    if (!propertiesFile.exists())
                        propertiesFile = defaultPropertiesFile;
                    if (!extraDDL.exists())
                        extraDDL = defaultExtraDDL;
                    File t3Results = new File (subdir, args[0] + ".t3expected");
                    if (t3Results.exists() && Types3Switch.ON) {
                        args[2] = fileContents(t3Results);
                    }
                    Object[] nargs = new Object[args.length+5];
                    nargs[0] = subdir.getName() + "/" + args[0];
                    nargs[1] = rulesFile;
                    nargs[2] = schemaFile;
                    nargs[3] = statsFile;
                    nargs[4] = propertiesFile;
                    nargs[5] = extraDDL;
                    System.arraycopy(args, 1, nargs, 6, args.length-1);
                    result.add(nargs);
                }
            }
        }
        return NamedParamsTestBase.namedCases(result);
    }

    public RulesTest(String caseName, 
                     File rulesFile, File schemaFile, File statsFile, File propertiesFile,
                     File extraDDL,
                     String sql, String expected, String error) {
        super(caseName, sql, expected, error);
        this.rulesFile = rulesFile;
        this.schemaFile = schemaFile;
        this.statsFile = statsFile;
        this.propertiesFile = propertiesFile;
        this.extraDDL = extraDDL;
    }

    protected RulesContext rules;

    @Before
    public void loadDDL() throws Exception {
        List<File> schemaFiles = new ArrayList<>(2);
        schemaFiles.add(schemaFile);
        if (extraDDL != null)
            schemaFiles.add(extraDDL);
        AkibanInformationSchema ais = loadSchema(schemaFiles);
        Properties properties = new Properties();
        if (propertiesFile != null) {
            FileInputStream fstr = new FileInputStream(propertiesFile);
            try {
                properties.load(fstr);
            }
            finally {
                fstr.close();
            }
        }
        rules = RulesTestContext.create(ais, statsFile, extraDDL != null,
                                        RulesTestHelper.loadRules(rulesFile), 
                                        properties);
        // Normally set as a consequence of OutputFormat.
        if (Boolean.parseBoolean(properties.getProperty("allowSubqueryMultipleColumns",
                                                        "false"))) {
            binder.setAllowSubqueryMultipleColumns(true);
            typeComputer = new NestedResultSetTypeComputer(new FunctionsRegistryImpl());
        }
        if (!Boolean.parseBoolean(properties.getProperty("useComposers", "true"))) {
            ((FunctionsTypeComputer)typeComputer).setUseComposers(false);
        }
    }

    @Test
    public void testRules() throws Exception {
        generateAndCheckResult();
    }

    @Override
    public String generateResult() throws Exception {
        StatementNode stmt = parser.parseStatement(sql);
        binder.bind(stmt);
        stmt = booleanNormalizer.normalize(stmt);
        typeComputer.compute(stmt);
        stmt = subqueryFlattener.flatten((DMLStatementNode)stmt);
        // Turn parsed AST into intermediate form as starting point.
        PlanContext plan = new PlanContext(rules, 
                                           new AST((DMLStatementNode)stmt,
                                                   parser.getParameterList()));
        rules.applyRules(plan);
        return PlanToString.of(plan.getPlan());
    }

    @Override
    public void checkResult(String result) throws IOException {
        assertEqualsWithoutHashes(caseName, expected, result);
    }

}
