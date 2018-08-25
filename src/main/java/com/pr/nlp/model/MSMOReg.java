package com.pr.nlp.model;

import com.pr.nlp.util.FileUtil;
import weka.classifiers.functions.SMOreg;
import weka.core.Debug;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.io.*;
import java.util.ArrayList;

public class MSMOReg {

    private SMOreg smo;

    public MSMOReg() {};

    public MSMOReg(SMOreg smo) {
        this.smo = smo;
    }

    public SMOreg getMoel() {
        return smo;
    }

    public void setModel(SMOreg smo) {
        this.smo = smo;
    }

    public void loadModel(String path) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(
                    new FileInputStream(new File(path)));
            smo = (SMOreg) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            smo = null;
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveModel(String path) {
        FileUtil.deleteFile(path);
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(
                    new FileOutputStream(path));
            oos.writeObject(smo);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void trainModel(Instances instances) {
        try {
            if (smo == null) this.smo = new SMOreg();
            smo.buildClassifier(instances);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double predict(Instance instance) {
        double result = -1;
        try {
            result = smo.classifyInstance(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public ArrayList<Double> predict(Instances instances) {
        ArrayList<Double> results = new ArrayList<>();
        for (Instance instance : instances) {
            try {
                results.add(smo.classifyInstance(instance));
            } catch (Exception e) {
                results.add(-1.0);
                e.printStackTrace();
            }
        }
        return results;
    }

}
