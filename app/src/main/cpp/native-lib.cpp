#include <jni.h>

extern "C" {

JNIEXPORT jdouble JNICALL
Java_com_example_pocketflow_viewmodel_TransactionViewModel_calculateTotal(
        JNIEnv* env, jobject /* this */,
        jdoubleArray amounts, jint size) {
    jdouble* arr = env->GetDoubleArrayElements(amounts, nullptr);
    jdouble total = 0.0;
    for (int i = 0; i < size; i++) total += arr[i];
    env->ReleaseDoubleArrayElements(amounts, arr, 0);
    return total;
}

JNIEXPORT jdouble JNICALL
Java_com_example_pocketflow_viewmodel_TransactionViewModel_calculateBudgetPercentage(
        JNIEnv* env, jobject /* this */,
        jdouble totalExpense, jdouble budgetLimit) {
    if (budgetLimit <= 0) return 0.0;
    return (totalExpense / budgetLimit) * 100.0;
}

JNIEXPORT jdouble JNICALL
Java_com_example_pocketflow_viewmodel_TransactionViewModel_calculateCategoryPercentage(
        JNIEnv* env, jobject /* this */,
        jdouble categoryTotal, jdouble grandTotal) {
    if (grandTotal <= 0) return 0.0;
    return (categoryTotal / grandTotal) * 100.0;
}

}