.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
includelib C:\masm32\lib\kernel32.lib
main PROTO

.data

.code

start:

invoke main
invoke ExitProcess,0

main PROC
mov eax, 10
ret
main ENDP
END start
