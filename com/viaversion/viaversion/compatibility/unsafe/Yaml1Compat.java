package com.viaversion.viaversion.compatibility.unsafe;

import com.viaversion.viaversion.compatibility.YamlCompat;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public final class Yaml1Compat implements YamlCompat {
    @Override
    public Representer createRepresenter(DumperOptions dumperOptions) {
        // $VF: Couldn't be decompiled
        // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
        // java.lang.RuntimeException: Constructor org/yaml/snakeyaml/representer/Representer.<init>()V not found
        //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExprUtil.getSyntheticParametersMask(ExprUtil.java:49)
        //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.appendParamList(InvocationExprent.java:982)
        //   at org.jetbrains.java.decompiler.modules.decompiler.exps.NewExprent.toJava(NewExprent.java:462)
        //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1054)
        //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:85)
        //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:925)
        //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:87)
        //   at org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement.toJava(RootStatement.java:36)
        //   at org.jetbrains.java.decompiler.main.ClassWriter.writeMethod(ClassWriter.java:1351)
        //
        // Bytecode:
        // 0: new org/yaml/snakeyaml/representer/Representer
        // 3: dup
        // 4: invokespecial org/yaml/snakeyaml/representer/Representer.<init> ()V
        // 7: areturn
    }

    @Override
    public SafeConstructor createSafeConstructor() {
        return new Yaml1Compat.CustomSafeConstructor();
    }

    private static final class CustomSafeConstructor extends SafeConstructor {
        public CustomSafeConstructor() {
            this.yamlClassConstructors.put(NodeId.mapping, new SafeConstructor.ConstructYamlMap());
            this.yamlConstructors.put(Tag.OMAP, new SafeConstructor.ConstructYamlOmap());
        }
    }
}
