int foo(int a, int b, int c);
int func2(int k, int b);
int main() {
<<<<<<< HEAD
	int k = 100;
	k /= 5;			 //40
	int b = k && 23;
	int c = func2(1, b); 	 //1
	return foo(k, 4, 2) / c; //5
}
int foo(int a, int b, int c){
	return a/b/c;   //5
}
int func2(int i, int j){
	int resF2 = i && j;
	return resF2;
=======
    for (int i = 0; ; i = i + 1) {
        // do something
    }
    return a;
>>>>>>> newFB
}